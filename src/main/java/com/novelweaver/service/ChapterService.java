package com.novelweaver.service;

/*
 * Chapter Service / 章节管理 / 章管理
 *
 * CN 章节同步、获取、列表
 * JP 章の同期、取得、一覧
 * EN Chapter sync, get, list
 */

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ChapterService {

    private static final Logger log = LoggerFactory.getLogger(ChapterService.class);
    private static final int CHUNK_TARGET = 700;
    private static final int OVERLAP_CP = 100;
    private final ProjectRepository projects;
    private final ChapterRepository chapters;
    private final ChapterVersionRepository versions;
    private final ChapterParagraphRepository paragraphs;
    private final Neo4jClient neo4j;
    private final WebClient meiliClient;


    public ChapterService(ProjectRepository projects, ChapterRepository chapters,
                          ChapterVersionRepository versions, ChapterParagraphRepository paragraphs,
                          Neo4jClient neo4j, WebClient.Builder wcb,
                          @Value("${novel.meili.url}") String meiliUrl,
                          @Value("${novel.meili.master-key}") String meiliKey) {
        this.projects = projects;
        this.chapters = chapters;
        this.versions = versions;
        this.paragraphs = paragraphs;
        this.neo4j = neo4j;
        this.meiliClient = wcb.baseUrl(meiliUrl)
                .defaultHeader("Authorization", "Bearer " + meiliKey)
                .build();
    }

    private static String textTruncate(String text, int maxCodePoints) {
        if (text == null) return "";
        int len = text.codePointCount(0, text.length());
        if (len <= maxCodePoints) return text;
        return text.substring(0, text.offsetByCodePoints(0, maxCodePoints)) + "…";
    }

    // ═══════════════════════════════════════════════
    // 分段
    // ═══════════════════════════════════════════════

    /*
     * 同步章节 / 同期 / Sync
     *
     * CN 保存/更新章节，自动分段→PG+Meilisearch+Neo4j
     * JP 章を保存/更新、自動分割→PG+Meilisearch+Neo4j
     * EN Save/update chapter, auto-segment→PG+Meilisearch+Neo4j
     */
    @McpTool(name = "chapter_sync", description = "保存/更新章节 — 自动分段 → embedding → PG + Meilisearch + Neo4j | CN 保存/更新章节 / JP 章を保存/更新 / EN Save/update chapter")
    @Transactional
    public ChapterSyncResult sync(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "章节号", required = true) int number,
            @McpToolParam(description = "标题", required = true) String title,
            @McpToolParam(description = "正文内容", required = true) String content,
            @McpToolParam(description = "阶段", required = false) String phase,
            @McpToolParam(description = "出场角色列表", required = false) List<String> characters,
            @McpToolParam(description = "出场物品列表", required = false) List<String> items,
            @McpToolParam(description = "段落向量（pgvector格式字符串，顺序与分段结果一致）", required = false) List<String> embeddings) {

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

        // 版本——null-safe unboxing
        Integer maxVer = versions.findMaxVersionByChapter(chapter);
        int newVer = (maxVer != null ? maxVer : 0) + 1;
        ChapterVersion ver = new ChapterVersion();
        ver.setChapter(chapter);
        ver.setVersion(newVer);
        ver.setContent(content);
        ver.setWordCount(chapter.getWordCount());
        ver.setCreatedAt(Instant.now());
        versions.save(ver);

        // 分段
        List<Segment> segs = segment(content);
        int paraCount = segs.size();

        // 删旧段 + 写新段
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

        // Meilisearch 索引
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
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            meiliOk = true;
        } catch (Exception e) {
            log.warn("Meilisearch indexing failed for chapter {}", chapter.getId(), e);
        }

        // Neo4j: MERGE chapter + :NEXT 链
        try {
            neo4j.query("""
                            MERGE (ch:Chapter {project_id: $pid, number: $num})
                            SET ch.title = $title, ch.phase = $phase, ch.status = 'draft',
                                ch.updatedAt = datetime()
                            """)
                    .bind(projectId).to("pid")
                    .bind(number).to("num")
                    .bind(title).to("title")
                    .bind(phase != null ? phase : "").to("phase")
                    .run();

            if (number > 1) {
                neo4j.query("""
                                MATCH (prev:Chapter {project_id: $pid, number: $prevNum})
                                MATCH (curr:Chapter {project_id: $pid, number: $num})
                                MERGE (prev)-[:NEXT]->(curr)
                                """)
                        .bind(projectId).to("pid")
                        .bind(number - 1).to("prevNum")
                        .bind(number).to("num")
                        .run();
            }
        } catch (Exception e) {
            log.warn("Neo4j chapter sync failed for chapter {}-{}", projectId, number, e);
        }

        // Neo4j: 人物出场关系
        if (characters != null && !characters.isEmpty()) {
            try {
                for (String name : characters) {
                    neo4j.query("""
                                    MERGE (c:Character {project_id: $pid, name: $name})
                                    """)
                            .bind(projectId).to("pid")
                            .bind(name).to("name")
                            .run();

                    neo4j.query("""
                                    MATCH (c:Character {project_id: $pid, name: $name})
                                    MATCH (ch:Chapter {project_id: $pid, number: $num})
                                    MERGE (c)-[:APPEARS_IN]->(ch)
                                    """)
                            .bind(projectId).to("pid")
                            .bind(name).to("name")
                            .bind(number).to("num")
                            .run();
                }
            } catch (Exception e) {
                log.warn("Neo4j character appearance sync failed for chapter {}-{}", projectId, number, e);
            }
        }

        // Neo4j: 物品出场关系
        if (items != null && !items.isEmpty()) {
            try {
                for (String itemName : items) {
                    neo4j.query("""
                                    MERGE (it:Item {project_id: $pid, name: $name})
                                    """)
                            .bind(projectId).to("pid")
                            .bind(itemName).to("name")
                            .run();

                    neo4j.query("""
                                    MATCH (it:Item {project_id: $pid, name: $name})
                                    MATCH (ch:Chapter {project_id: $pid, number: $num})
                                    MERGE (it)-[:APPEARS_IN]->(ch)
                                    """)
                            .bind(projectId).to("pid")
                            .bind(itemName).to("name")
                            .bind(number).to("num")
                            .run();
                }
            } catch (Exception e) {
                log.warn("Neo4j item appearance sync failed for chapter {}-{}", projectId, number, e);
            }
        }

        long elapsed = System.currentTimeMillis() - t0;

        return new ChapterSyncResult(
                "ok", chapter.getId().toString(), newVer, paraCount, elapsed,
                meiliOk, null);
    }

    /*
     * 获取章节 / 取得 / Get
     *
     * CN 获取单章正文
     * JP 単章の本文を取得
     * EN Get single chapter content
     */
    @McpTool(name = "chapter_get", description = "获取章节正文 | CN 获取章节正文 / JP 章の本文を取得 / EN Get chapter content")
    public ChapterGetResult get(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "章节号", required = true) int number) {
        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        Chapter ch = chapters.findByProjectAndChapterNumber(proj, number).orElse(null);
        if (ch == null) {
            return new ChapterGetResult("not_found", null, null, null);
        }
        return new ChapterGetResult("ok", ch.getId().toString(), ch.getTitle(), ch.getContent());
    }

    /*
     * 章节列表 / 一覧 / List
     *
     * CN 列出项目所有章节
     * JP プロジェクトの全章を一覧表示
     * EN List all chapters in project
     */
    @McpTool(name = "chapter_list", description = "列出所有章节 | CN 列出所有章节 / JP 全章を一覧表示 / EN List all chapters")
    public ChapterListResult list(
            @McpToolParam(description = "项目ID", required = true) String projectId) {
        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        List<Chapter> chs = chapters.findByProjectOrderByChapterNumber(proj);
        List<ChapterInfo> infos = chs.stream()
                .map(c -> new ChapterInfo(c.getChapterNumber(), c.getTitle(), c.getStatus(), c.getWordCount()))
                .toList();
        return new ChapterListResult(infos, null);
    }

    List<Segment> segment(String content) {
        List<Segment> result = new ArrayList<>();
        String[] scenes = content.split("\n(?=## )");
        for (String block : scenes) {
            String[] lines = block.split("\n", 2);
            String headline = lines[0].replaceAll("^#+\\s*", "").trim();
            String body = lines.length > 1 ? lines[1] : "";

            int len = body.codePointCount(0, body.length());
            if (len <= 800) {
                result.add(new Segment(headline, block, classify(body)));
            } else {
                result.addAll(splitChunks(headline, body, len));
            }
        }
        return result;
    }

    private List<Segment> splitChunks(String scene, String body, int totalCodePoints) {
        List<Segment> chunks = new ArrayList<>();
        int pos = 0;
        int seq = 0;
        int seen = 0;
        while (pos < body.length()) {
            int remaining = totalCodePoints - seen;
            int chunkChars = Math.min(CHUNK_TARGET, remaining);
            int end = findBreak(body, pos, chunkChars);
            String chunk = body.substring(pos, end).trim();
            int chunkCP = chunk.codePointCount(0, chunk.length());
            chunks.add(new Segment(scene + " [" + (seq + 1) + "]", chunk, classify(chunk)));
            pos = end;
            seq++;
            seen += chunkCP;
            if (pos >= body.length()) break;
            int back = overlapBack(body, pos);
            pos = Math.max(pos, back);
        }
        return chunks;
    }

    private int findBreak(String text, int start, int targetChars) {
        int searchEnd = Math.min(text.length(), start + targetChars + 100);
        if (searchEnd >= text.length()) return text.length();
        for (int i = start + targetChars - 50; i < searchEnd; i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '。' || c == '？' || c == '！' || c == '」') {
                return i + 1;
            }
        }
        return Math.min(text.length(), start + targetChars);
    }

    private int overlapBack(String text, int pos) {
        int count = 0;
        for (int i = pos - 1; i >= 0 && count < OVERLAP_CP; i--) {
            count += Character.charCount(text.codePointAt(i));
            if (count >= OVERLAP_CP) return i;
        }
        return Math.max(0, pos - OVERLAP_CP);
    }

    private String classify(String text) {
        int quotes = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '"' || text.charAt(i) == '“' || text.charAt(i) == '”') quotes++;
        }
        int total = text.codePointCount(0, text.length());
        return (quotes > 0 && total > 0 && (double) quotes / total > 0.05) ? "dialogue" : "narrative";
    }

    record Segment(String scene, String text, String type) {
    }

    // ── result records ──

    public record ChapterSyncResult(String status, String chapterId, int version,
                                    int paragraphCount, long processingMs,
                                    boolean meilisearchIndexed, String note) {
    }

    public record ChapterGetResult(String status, String chapterId, String title, String content) {
    }

    public record ChapterListResult(List<ChapterInfo> chapters, String note) {
    }

    public record ChapterInfo(int number, String title, String status, Integer wordCount) {
    }
}
