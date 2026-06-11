package com.novelweaver.service;

/*
 * RAG Service / 语义搜索 / 意味検索
 *
 * CN 向量搜索、模糊搜索、语义检索
 * JP ベクトル検索、曖昧検索、意味検索
 * EN Vector search, fuzzy search, semantic retrieval
 */

import com.novelweaver.model.Chapter;
import com.novelweaver.model.ChapterParagraph;
import com.novelweaver.model.Project;
import com.novelweaver.repository.ChapterParagraphRepository;
import com.novelweaver.repository.ChapterRepository;
import com.novelweaver.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class RAGService {

    private static final Logger log = LoggerFactory.getLogger(RAGService.class);
    private static final int DEFAULT_K = 5;

    private final ChapterParagraphRepository paragraphs;
    private final ChapterRepository chapters;
    private final ProjectRepository projects;
    private final WebClient meiliClient;

    public RAGService(ChapterParagraphRepository paragraphs, ChapterRepository chapters,
                      ProjectRepository projects, WebClient.Builder wcb,
                      @Value("${novel.meili.url}") String meiliUrl,
                      @Value("${novel.meili.master-key}") String meiliKey) {
        this.paragraphs = paragraphs;
        this.chapters = chapters;
        this.projects = projects;
        this.meiliClient = wcb.baseUrl(meiliUrl)
                .defaultHeader("Authorization", "Bearer " + meiliKey)
                .build();
    }

    private static String textSnippet(String text, int maxCodePoints) {
        if (text == null) return "";
        int len = text.codePointCount(0, text.length());
        if (len <= maxCodePoints) return text;
        int safeLen = text.offsetByCodePoints(0, maxCodePoints * 3 / 2);
        return text.substring(0, Math.min(text.length(), safeLen)) + "…";
    }

    /*
     * 语义搜索 / 意味検索 / RAG search
     *
     * CN 语义搜索已写内容（pgvector + 可选合成）
     * JP 既存内容を意味検索（pgvector + 合成オプション）
     * EN Semantic search existing content (pgvector + optional synthesis)
     */
    @McpTool(name = "rag_search", description = "语义搜索 — pgvector 向量检索 + 可选合成 | CN 语义搜索 / JP 意味検索 / EN Semantic search")
    public RAGSearchResult search(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "查询文本（用于 Meilisearch 辅助检索和日志）", required = true) String query,
            @McpToolParam(description = "bge-m3 向量（1024维，pgvector 格式字符串）", required = true) String embedding,
            @McpToolParam(description = "返回结果数（默认5）", required = false) Integer k,
            @McpToolParam(description = "是否合成回答（默认false）", required = false) Boolean synthesize) {

        UUID pid = UUID.fromString(projectId);
        int limit = k != null ? k : DEFAULT_K;

        List<ChapterParagraph> vectorHits = paragraphs.findSimilar(pid, embedding, limit);
        List<UUID> ids = vectorHits.stream().map(ChapterParagraph::getId).toList();
        Map<UUID, ChapterParagraph> withChapters = new LinkedHashMap<>();
        if (!ids.isEmpty()) {
            paragraphs.findByIdInWithChapter(ids)
                    .forEach(cp -> withChapters.put(cp.getId(), cp));
        }

        List<ParagraphHit> hits = vectorHits.stream()
                .map(cp -> {
                    ChapterParagraph resolved = withChapters.getOrDefault(cp.getId(), cp);
                    String title = resolved.getChapter() != null ? resolved.getChapter().getTitle() : "未知章节";
                    int chNum = resolved.getChapter() != null ? resolved.getChapter().getChapterNumber() : 0;
                    return new ParagraphHit(title, chNum, cp.getSeq(), cp.getScene(), cp.getContent(), cp.getSceneType());
                })
                .toList();

        return new RAGSearchResult(hits.size(), hits, false,
                Boolean.TRUE.equals(synthesize) ? "synthesis pending — Phase 2" : null);
    }

    /*
     * 向量搜索 / ベクトル検索 / Vector search
     *
     * CN 纯向量搜索，不做合成
     * JP ベクトルのみ検索、合成なし
     * EN Pure vector search, no synthesis
     */
    @McpTool(name = "semantic_search", description = "纯向量搜索 — 不做合成 | CN 纯向量搜索 / JP ベクトル検索 / EN Vector-only search")
    public RAGSearchResult semantic(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "bge-m3 向量（1024维，pgvector 格式字符串）", required = true) String embedding,
            @McpToolParam(description = "返回数", required = false) Integer k) {

        UUID pid = UUID.fromString(projectId);
        int limit = k != null ? k : DEFAULT_K;

        List<ChapterParagraph> vectorHits = paragraphs.findSimilar(pid, embedding, limit);
        List<UUID> ids = vectorHits.stream().map(ChapterParagraph::getId).toList();
        Map<UUID, ChapterParagraph> withChapters = new LinkedHashMap<>();
        if (!ids.isEmpty()) {
            paragraphs.findByIdInWithChapter(ids)
                    .forEach(cp -> withChapters.put(cp.getId(), cp));
        }

        List<ParagraphHit> hits = vectorHits.stream()
                .map(cp -> {
                    ChapterParagraph resolved = withChapters.getOrDefault(cp.getId(), cp);
                    String title = resolved.getChapter() != null ? resolved.getChapter().getTitle() : "未知章节";
                    int chNum = resolved.getChapter() != null ? resolved.getChapter().getChapterNumber() : 0;
                    return new ParagraphHit(title, chNum, cp.getSeq(), cp.getScene(), cp.getContent(), cp.getSceneType());
                })
                .toList();

        return new RAGSearchResult(hits.size(), hits, false, null);
    }

    /*
     * 模糊搜索 / 曖昧検索 / Fuzzy search
     *
     * CN 关键词模糊搜索（Meilisearch，降级 PG ILIKE）
     * JP キーワード曖昧検索（Meilisearch、PG ILIKEにフォールバック）
     * EN Keyword fuzzy search (Meilisearch, fallback to PG ILIKE)
     */
    @McpTool(name = "fuzzy_search", description = "模糊关键词搜索 — Meilisearch（typo tolerance），不可用时降级 PG ILIKE | CN 模糊搜索 / JP 曖昧検索 / EN Fuzzy search")
    public FuzzySearchResult fuzzy(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "关键词", required = true) String keyword,
            @McpToolParam(description = "返回数", required = false) Integer limit) {

        // Validate UUID format to prevent filter injection
        UUID.fromString(projectId);

        int n = limit != null ? limit : 10;

        try {
            MeiliSearchResponse meiliResp = meiliClient.post()
                    .uri("/indexes/novel_chapters/search")
                    .bodyValue(Map.of(
                            "q", keyword,
                            "limit", n,
                            "filter", "project_id = " + projectId,
                            "attributesToRetrieve", List.of("title", "chapter_number", "content", "phase")))
                    .retrieve()
                    .bodyToMono(MeiliSearchResponse.class)
                    .block();

            if (meiliResp != null && meiliResp.hits != null) {
                List<FuzzyHit> hits = meiliResp.hits.stream()
                        .map(h -> new FuzzyHit(h.title, h.chapterNumber, textSnippet(h.content, 200), h.phase))
                        .toList();
                return new FuzzySearchResult(hits.size(), hits, "meilisearch");
            }
        } catch (Exception e) {
            log.warn("Meilisearch fuzzy search failed, falling back to PG ILIKE (keyword={})", keyword, e);
        }

        List<FuzzyHit> pgHits = fuzzyIlike(projectId, keyword, n);
        return new FuzzySearchResult(pgHits.size(), pgHits, "pg_ilike");
    }

    private List<FuzzyHit> fuzzyIlike(String projectId, String keyword, int limit) {
        Project proj = projects.findById(UUID.fromString(projectId)).orElse(null);
        if (proj == null) return List.of();

        List<Chapter> all = chapters.findByProjectOrderByChapterNumber(proj);
        List<FuzzyHit> result = new ArrayList<>();
        Pattern pattern = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE);
        for (Chapter ch : all) {
            if (ch.getContent() != null && ch.getContent().contains(keyword)) {
                String snip = textSnippet(ch.getContent(), 200);
                snip = pattern.matcher(snip).replaceAll("**$0**");
                result.add(new FuzzyHit(ch.getTitle(), ch.getChapterNumber(), snip, ch.getPhase()));
                if (result.size() >= limit) break;
            }
        }
        return result;
    }

    @SuppressWarnings("unused")
    private static class MeiliSearchResponse {
        public List<MeiliHit> hits;
    }

    @SuppressWarnings("unused")
    private static class MeiliHit {
        public String title;
        public int chapterNumber;
        public String content;
        public String phase;
    }

    // ── result records ──

    public record RAGSearchResult(int count, List<ParagraphHit> hits,
                                  boolean synthesized, String synthesis) {
    }

    public record ParagraphHit(String chapterTitle, int chapterNumber, int paragraphSeq,
                               String scene, String text, String sceneType) {
    }

    public record FuzzySearchResult(int count, List<FuzzyHit> hits, String source) {
    }

    public record FuzzyHit(String chapterTitle, int chapterNumber, String snippet, String phase) {
    }
}
