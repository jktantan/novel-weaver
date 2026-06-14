package com.novelweaver.service;

import com.arcadedb.remote.RemoteDatabase;
import com.novelweaver.config.ArcadeDBManager;
import com.novelweaver.model.Chapter;
import com.novelweaver.model.ChapterParagraph;
import com.novelweaver.model.ChapterVersion;
import com.novelweaver.model.Project;
import com.novelweaver.repository.ChapterParagraphRepository;
import com.novelweaver.repository.ChapterRepository;
import com.novelweaver.repository.ChapterVersionRepository;
import com.novelweaver.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ChapterService {

    private static final Logger log = LoggerFactory.getLogger(ChapterService.class);

    private final ChapterRepository chapters;
    private final ChapterVersionRepository versions;
    private final ChapterParagraphRepository paragraphs;
    private final ProjectRepository projects;
    private final ArcadeDBManager arcadeDB;
    private final WebClient meiliClient;

    public ChapterService(ChapterRepository chapters, ChapterVersionRepository versions,
                          ChapterParagraphRepository paragraphs, ProjectRepository projects,
                          ArcadeDBManager arcadeDB, WebClient.Builder wcb,
                          @org.springframework.beans.factory.annotation.Value("${novel.meili.url}") String meiliUrl,
                          @org.springframework.beans.factory.annotation.Value("${novel.meili.master-key}") String meiliKey) {
        this.chapters = chapters;
        this.versions = versions;
        this.paragraphs = paragraphs;
        this.projects = projects;
        this.arcadeDB = arcadeDB;
        this.meiliClient = wcb.baseUrl(meiliUrl).defaultHeader("Authorization", "Bearer " + meiliKey).build();
    }

    public static List<Segment> segment(String content) {
        if (content == null) return List.of();
        if (content.isEmpty()) return List.of(new Segment("", "", ""));

        // Normalize line endings
        String normalized = content.replace("\r\n", "\n").replace("\r", "\n");

        // Split at heading positions using zero-width lookahead
        Pattern splitPat = Pattern.compile("(?m)^(?=#{1,6}\\s)");
        String[] sections = splitPat.split(normalized);

        Pattern headingPat = Pattern.compile("^(#{1,6})\\s+(.*)");
        List<Segment> segs = new ArrayList<>();

        for (String section : sections) {
            String trimmed = section.strip();
            if (trimmed.isEmpty()) continue;

            Matcher hm = headingPat.matcher(trimmed);
            if (hm.find()) {
                String heading = hm.group(2).strip();
                segs.add(new Segment(heading, trimmed, ""));
            } else {
                segs.add(new Segment(trimmed, trimmed, ""));
            }
        }

        if (segs.isEmpty()) {
            // Whitespace-only input
            return List.of(new Segment("", "", ""));
        }

        // Chunk long segments
        return chunkLong(segs);
    }

    private static List<Segment> chunkLong(List<Segment> segs) {
        List<Segment> result = new ArrayList<>();
        for (Segment s : segs) {
            int cpCount = s.text().codePointCount(0, s.text().length());
            if (cpCount <= 800) {
                result.add(s);
            } else {
                String text = s.text();
                int[] codepoints = text.codePoints().toArray();
                int total = codepoints.length;
                int pos = 0;
                int chunkNo = 0;
                while (pos < total) {
                    int end = Math.min(pos + 800, total);
                    StringBuilder sb = new StringBuilder();
                    for (int i = pos; i < end; i++) {
                        sb.appendCodePoint(codepoints[i]);
                    }
                    chunkNo++;
                    String sceneName = chunkNo == 1 ? s.scene() : s.scene() + "[" + chunkNo + "]";
                    result.add(new Segment(sceneName, sb.toString(), ""));
                    pos = end;
                }
            }
        }
        return result;
    }

    public static String textTruncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        if (maxLen == 0) return "\u2026";
        return text.substring(0, maxLen) + "\u2026";
    }

    @McpTool(name = "chapter_sync", description = "保存/更新章节 | CN 保存/更新章节 / JP 章を保存/更新 / EN Save/update chapter")
    @Transactional
    public ChapterSyncResult sync(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "章节号", required = true) int number,
            @McpToolParam(description = "标题", required = true) String title,
            @McpToolParam(description = "正文内容", required = true) String content,
            @McpToolParam(description = "阶段", required = false) String phase,
            @McpToolParam(description = "出场角色列表", required = false) List<String> characters,
            @McpToolParam(description = "出场物品列表", required = false) List<String> items,
            @McpToolParam(description = "出场地点列表", required = false) List<String> locations,
            @McpToolParam(description = "段落向量", required = false) List<String> embeddings) {

        long t0 = System.currentTimeMillis();

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        Chapter chapter = chapters.findByProjectAndChapterNumber(proj, number)
                .orElseGet(() -> {
                    Chapter c = new Chapter();
                    c.setProject(proj);
                    c.setChapterNumber(number);
                    return c;
                });
        chapter.setTitle(title);
        chapter.setContent(content);
        chapter.setPhase(phase);
        chapter.setWordCount(content.codePointCount(0, content.length()));
        chapter.setStatus("draft");
        chapter.setUpdatedAt(Instant.now());
        chapter = chapters.save(chapter);

        Integer maxVer = versions.findMaxVersionByChapter(chapter);
        int newVer = (maxVer != null ? maxVer : 0) + 1;
        ChapterVersion ver = new ChapterVersion();
        ver.setChapter(chapter);
        ver.setVersion(newVer);
        ver.setContent(content);
        ver.setWordCount(chapter.getWordCount());
        ver.setCreatedAt(Instant.now());
        versions.save(ver);

        List<Segment> segs = segment(content);
        int paraCount = segs.size();

        List<ChapterParagraph> oldParas = paragraphs.findByChapterOrderBySeq(chapter);
        paragraphs.deleteAll(oldParas);

        List<ChapterParagraph> batch = new ArrayList<>();
        int embCount = embeddings != null ? embeddings.size() : 0;
        for (int i = 0; i < segs.size(); i++) {
            Segment s = segs.get(i);
            ChapterParagraph cp = new ChapterParagraph();
            cp.setVersion(ver);
            cp.setChapter(chapter);
            cp.setProject(proj);
            cp.setSeq(i + 1);
            cp.setScene(s.scene);
            cp.setContent(s.text);
            cp.setSceneType(s.type);
            cp.setEmbedding(i < embCount ? embeddings.get(i) : null);
            cp.setCreatedAt(Instant.now());
            batch.add(cp);
        }
        paragraphs.saveAll(batch);
        paragraphs.flush();

        boolean meiliOk = false;
        try {
            meiliClient.post()
                    .uri("/indexes/novel_chapters/documents")
                    .bodyValue(List.of(Map.of(
                            "id", "ch-" + chapter.getId(),
                            "project_id", projectId,
                            "chapter_number", number,
                            "title", title,
                            "content", textTruncate(content, 10000),
                            "phase", phase != null ? phase : "")))
                    .retrieve().toBodilessEntity().block();
            meiliOk = true;
        } catch (Exception e) {
            log.warn("Meilisearch indexing failed for chapter {}", chapter.getId(), e);
        }

        // ArcadeDB: chapter + NEXT chain + appearance relationships
        List<String> arcErrors = new ArrayList<>();
        try (RemoteDatabase db = arcadeDB.open(projectId)) {
            db.command("cypher", """
                    MERGE (ch:Chapter {number: $num})
                    SET ch.title = $title, ch.phase = $phase, ch.status = 'draft', ch.updatedAt = datetime()
                    """, Map.of("num", number, "title", title, "phase", phase != null ? phase : ""));

            if (number > 1) {
                db.command("cypher", """
                        MATCH (prev:Chapter {number: $prevNum})
                        MATCH (curr:Chapter {number: $num})
                        MERGE (prev)-[:NEXT]->(curr)
                        """, Map.of("prevNum", number - 1, "num", number));
            }
        } catch (Exception e) {
            log.warn("ArcadeDB chapter sync failed for {}-{}", projectId, number, e);
            arcErrors.add("chapter: " + e.getMessage());
        }

        // Character appearances
        if (characters != null && !characters.isEmpty()) {
            try (RemoteDatabase db = arcadeDB.open(projectId)) {
                for (String name : characters) {
                    db.command("cypher", """
                            MERGE (c:Character {name: $name})
                            SET c.identity = coalesce(c.identity, '{}')
                            WITH c
                            MATCH (ch:Chapter {number: $num})
                            MERGE (c)-[:APPEARS_IN]->(ch)
                            """, Map.of("name", name, "num", number));
                }
            } catch (Exception e) {
                log.warn("ArcadeDB character appearance sync failed for {}-{}", projectId, number, e);
                arcErrors.add("characters: " + e.getMessage());
            }
        }

        // Item appearances
        if (items != null && !items.isEmpty()) {
            try (RemoteDatabase db = arcadeDB.open(projectId)) {
                for (String name : items) {
                    db.command("cypher", """
                            MERGE (it:Item {name: $name})
                            SET it.identity = coalesce(it.identity, '{}')
                            WITH it
                            MATCH (ch:Chapter {number: $num})
                            MERGE (it)-[:APPEARS_IN]->(ch)
                            """, Map.of("name", name, "num", number));
                }
            } catch (Exception e) {
                log.warn("ArcadeDB item appearance sync failed for {}-{}", projectId, number, e);
                arcErrors.add("items: " + e.getMessage());
            }
        }

        // Location appearances
        if (locations != null && !locations.isEmpty()) {
            try (RemoteDatabase db = arcadeDB.open(projectId)) {
                for (String name : locations) {
                    db.command("cypher", """
                            MERGE (loc:Location {name: $name})
                            SET loc.identity = coalesce(loc.identity, '{}')
                            WITH loc
                            MATCH (ch:Chapter {number: $num})
                            MERGE (loc)-[:APPEARS_IN]->(ch)
                            """, Map.of("name", name, "num", number));
                }
            } catch (Exception e) {
                log.warn("ArcadeDB location appearance sync failed for {}-{}", projectId, number, e);
                arcErrors.add("locations: " + e.getMessage());
            }
        }

        long elapsed = System.currentTimeMillis() - t0;
        return new ChapterSyncResult("ok", chapter.getId().toString(), newVer, paraCount, elapsed,
                meiliOk, arcErrors.isEmpty() ? null : String.join("; ", arcErrors));
    }

    @McpTool(name = "chapter_get", description = "获取章节正文 | CN 获取章节正文 / JP 章の本文を取得 / EN Get chapter content")
    public ChapterGetResult get(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "章节号", required = true) int number) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        Chapter ch = chapters.findByProjectAndChapterNumber(proj, number)
                .orElseThrow(() -> new IllegalArgumentException("Chapter not found: " + number));

        return new ChapterGetResult(ch.getChapterNumber(), ch.getTitle(), ch.getContent(),
                ch.getWordCount(), ch.getPhase(), ch.getStatus());
    }

    @McpTool(name = "chapter_list", description = "列出所有章节 | CN 列出所有章节 / JP 全章を一覧表示 / EN List all chapters")
    public ChapterListResult list(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "返回数量上限", required = false) Integer limit,
            @McpToolParam(description = "偏移量", required = false) Integer offset) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        List<Chapter> all = chapters.findByProjectOrderByChapterNumber(proj);
        int off = offset != null ? Math.max(0, offset) : 0;
        int lim = limit != null ? Math.max(1, Math.min(limit, 100)) : 50;

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = off; i < Math.min(all.size(), off + lim); i++) {
            Chapter c = all.get(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("number", c.getChapterNumber());
            m.put("title", c.getTitle());
            m.put("word_count", c.getWordCount());
            m.put("phase", c.getPhase());
            m.put("status", c.getStatus());
            result.add(m);
        }
        return new ChapterListResult(all.size(), result);
    }

    public static String classify(String text) {
        if (text == null || text.isEmpty()) return "narrative";
        long quoteCount = text.chars().filter(c -> c == '"' || c == '\u201C' || c == '\u201D').count();
        return (double) quoteCount / text.length() > 0.05 ? "dialogue" : "narrative";
    }

    public record Segment(String scene, String text, String type) {
    }

    public record ChapterSyncResult(String status, String chapterId, int version, int paragraphCount,
                                    long elapsedMs, boolean meilisearchOk, String arcadeDbErrors) {
    }

    public record ChapterGetResult(int number, String title, String content, int wordCount, String phase,
                                   String status) {
    }

    public record ChapterListResult(int total, List<Map<String, Object>> chapters) {
    }
}
