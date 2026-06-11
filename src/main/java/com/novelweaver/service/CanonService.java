package com.novelweaver.service;

/*
 * Canon Service / 正典管理 / 正典管理
 *
 * CN 正典资料导入、搜索、审核、走向追踪
 * JP 正典資料取込、検索、検証、展開追跡
 * EN Canon import, search, verify, status tracking
 */

import com.novelweaver.model.*;
import com.novelweaver.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class CanonService {

    private static final Logger log = LoggerFactory.getLogger(CanonService.class);
    private static final int MAX_SEARCH_RESULTS = 50;

    private final CanonSourceRepository sources;
    private final CanonCharacterRepository canonChars;
    private final CanonEventRepository canonEvents;
    private final CanonRelationshipRepository canonRels;
    private final CanonEventStatusRepository canonStatus;
    private final ProjectRepository projects;

    public CanonService(CanonSourceRepository sources, CanonCharacterRepository canonChars,
                        CanonEventRepository canonEvents, CanonRelationshipRepository canonRels,
                        CanonEventStatusRepository canonStatus, ProjectRepository projects) {
        this.sources = sources;
        this.canonChars = canonChars;
        this.canonEvents = canonEvents;
        this.canonRels = canonRels;
        this.canonStatus = canonStatus;
        this.projects = projects;
    }


    /*
     * 导入正典 / 取り込み / Import
     *
     * CN 导入同人正典资料（文本→结构化）
     * JP 二次創作正典資料を取り込む（テキスト→構造化）
     * EN Import fanfic canon data (text→structured)
     */
    @McpTool(name = "canon_import", description = "导入同人正典资料 | CN 导入正典资料 / JP 正典資料を取り込む / EN Import canon data")
    @Transactional
    public CanonImportResult importData(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "来源名称", required = true) String sourceName,
            @McpToolParam(description = "正典文本", required = true) String text) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        // 按项目+名称精准查找
        List<CanonSource> existing = sources.findByProjectAndName(proj, sourceName);
        CanonSource src = existing.isEmpty() ? new CanonSource() : existing.get(0);

        src.setProject(proj);
        src.setName(sourceName);
        if (existing.isEmpty()) {
            src.setUrl(null);
            src.setVerified(false);
            src.setCreatedAt(Instant.now());
        }
        sources.save(src);

        return new CanonImportResult("ok", src.getId().toString(), sourceName,
                text.codePointCount(0, text.length()) + " 字已接收。结构化解析待后续实现。");
    }


    /*
     * 搜索正典 / 検索 / Search
     *
     * CN 搜索正典人物和事件（向量+关键词）
     * JP 正典キャラクターとイベントを検索（ベクトル+キーワード）
     * EN Search canon characters and events (vector+keyword)
     */
    @McpTool(name = "canon_search", description = "搜索正典资料 | CN 搜索正典资料 / JP 正典資料を検索 / EN Search canon data")
    public CanonSearchResult search(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "查询", required = true) String query,
            @McpToolParam(description = "bge-m3 向量（1024维，pgvector 格式）", required = false) String embedding) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        List<CanonEvent> eventHits;
        if (embedding != null && !embedding.isBlank()) {
            eventHits = canonEvents.findSimilar(UUID.fromString(projectId), embedding, MAX_SEARCH_RESULTS);
        } else {
            eventHits = canonEvents.findByProject(proj);
        }

        List<CanonCharacter> chars = canonChars.findByProject(proj);

        return new CanonSearchResult(
                chars.stream().map(c -> new CanonCharInfo(c.getId().toString(), c.getName(),
                        c.getAliases(), c.getBio(), c.getVerified())).toList(),
                eventHits.stream().map(e -> new CanonEventInfo(e.getId().toString(), e.getName(),
                        e.getTimelinePos(), e.getDateLabel(), e.getCanonLevel(), e.getDescription(), e.getVerified())).toList());
    }


    /*
     * 审核正典 / 検証 / Verify
     *
     * CN 标记正典条目已人工审核
     * JP 正典エントリを人手確認済みにマーク
     * EN Mark canon entry as human-verified
     */
    @McpTool(name = "canon_verify", description = "标记正典条目已审核 | CN 标记正典已审核 / JP 正典を確認済みに / EN Mark canon as verified")
    @Transactional
    public CanonVerifyResult verify(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "条目ID（UUID）", required = true) String entityId) {

        UUID id = UUID.fromString(entityId);

        boolean updated = false;
        String type = "unknown";

        var src = sources.findById(id).orElse(null);
        if (src != null) {
            src.setVerified(true);
            sources.save(src);
            updated = true;
            type = "source";
        }

        if (!updated) {
            var ch = canonChars.findById(id).orElse(null);
            if (ch != null) {
                ch.setVerified(true);
                canonChars.save(ch);
                updated = true;
                type = "character";
            }
        }
        if (!updated) {
            var ev = canonEvents.findById(id).orElse(null);
            if (ev != null) {
                ev.setVerified(true);
                canonEvents.save(ev);
                updated = true;
                type = "event";
            }
        }
        if (!updated) {
            var rel = canonRels.findById(id).orElse(null);
            if (rel != null) {
                rel.setVerified(true);
                canonRels.save(rel);
                updated = true;
                type = "relationship";
            }
        }

        return new CanonVerifyResult(updated ? "ok" : "not_found", entityId, updated, type);
    }


    /*
     * 正典走向 / 展開 / Status
     *
     * CN 设置正典事件在同人中的实际走向
     * JP 正典イベントの二次創作における実際の展開を設定
     * EN Set canon event status in fanfic (pending/triggered/modified/skipped)
     */
    @McpTool(name = "canon_status_set", description = "设置正典事件在同人中的实际走向——记录该事件是否按正典发生/被修改/被跳过 | CN 设置正典走向 / JP 正典展開を設定 / EN Set canon event status")
    @Transactional
    public CanonStatusSetResult statusSet(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "正典事件ID", required = true) String canonEventId,
            @McpToolParam(description = "状态 pending(未触发) / triggered(按正典发生) / modified(被改变) / skipped(跳过)", required = true) String status,
            @McpToolParam(description = "实际发生了什么（与正典不同时）", required = false) String actualDescription,
            @McpToolParam(description = "发生在哪一章", required = false) Integer occurredInChapter,
            @McpToolParam(description = "为什么偏离正典", required = false) String divergenceReason) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        CanonEvent ev = canonEvents.findById(UUID.fromString(canonEventId))
                .orElseThrow(() -> new IllegalArgumentException("Canon event not found: " + canonEventId));

        CanonEventStatus ces = canonStatus.findByProjectAndCanonEvent(proj, ev)
                .orElseGet(() -> {
                    CanonEventStatus s = new CanonEventStatus();
                    s.setProject(proj);
                    s.setCanonEvent(ev);
                    s.setCreatedAt(Instant.now());
                    return s;
                });

        ces.setStatus(status);
        if (actualDescription != null) ces.setActualDescription(actualDescription);
        if (occurredInChapter != null) ces.setOccurredInChapter(occurredInChapter);
        if (divergenceReason != null) ces.setDivergenceReason(divergenceReason);
        ces.setUpdatedAt(Instant.now());
        canonStatus.save(ces);

        return new CanonStatusSetResult("ok", canonEventId, ev.getName(), status);
    }

    // ── result records ──

    public record CanonImportResult(String status, String sourceId, String sourceName, String note) {
    }

    public record CanonSearchResult(List<CanonCharInfo> characters, List<CanonEventInfo> events) {
    }

    public record CanonCharInfo(String id, String name, String[] aliases, String bio, Boolean verified) {
    }

    public record CanonEventInfo(String id, String name, String timelinePos, String dateLabel,
                                 String canonLevel, String description, Boolean verified) {
    }

    public record CanonVerifyResult(String status, String entityId, boolean verified, String type) {
    }

    public record CanonStatusSetResult(String status, String canonEventId, String eventName, String newStatus) {
    }
}
