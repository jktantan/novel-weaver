package com.novelweaver.service;

/*
 * Project Service / 项目管理 / プロジェクト管理
 *
 * CN 项目创建、归档、删除、备份、恢复
 * JP プロジェクト作成、アーカイブ、削除、バックアップ、復元
 * EN Project init, archive, delete, backup, restore
 */

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelweaver.model.Project;
import com.novelweaver.model.Universe;
import com.novelweaver.repository.ProjectRepository;
import com.novelweaver.repository.UniverseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import java.util.*;

@Component
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    // ── Tables with direct project_id ──
    private static final String[] PG_DIRECT_TABLES = {
            "character_voiceprints",
            "deduction_logs",
            "chapter_paragraphs",
            "character_relationships",
            "foreshadowing_index",
            "canon_event_status",
            "canon_relationships",
            "canon_events",
            "canon_characters",
            "canon_sources",
            "character_snapshots",
            "character_profiles",
            "timeline_events",
            "timelines",
            "timeline_links",
            "universe_relations",
            "universes",
            "locations",
            "items",
            "chapters",
    };
    // ── Tables with indirect FK (no project_id) — join via parent table ──
    private static final String[][] PG_JOIN_TABLES = {
            {"chapter_versions", "chapter_id", "chapters"},
    };
    private final ProjectRepository projects;
    private final UniverseRepository universes;
    private final UniverseService universeService;
    private final Neo4jClient neo4j;
    private final WebClient meiliClient;
    @PersistenceContext
    private EntityManager em;

    public ProjectService(ProjectRepository projects, UniverseRepository universes,
                          UniverseService universeService, Neo4jClient neo4j,
                          WebClient.Builder wcb,
                          @Value("${novel.meili.url}") String meiliUrl,
                          @Value("${novel.meili.master-key}") String meiliKey) {
        this.projects = projects;
        this.universes = universes;
        this.universeService = universeService;
        this.neo4j = neo4j;
        this.meiliClient = wcb.baseUrl(meiliUrl)
                .defaultHeader("Authorization", "Bearer " + meiliKey)
                .build();
    }


    /*
     * 初始化项目 / プロジェクト初期化 / Init project
     *
     * CN 创建新项目，写入 PG + Neo4j
     * JP 新規プロジェクトを作成、PG+Neo4jに書き込み
     * EN Create a new project, write to PG + Neo4j
     */
    @McpTool(name = "project_init", description = "创建新项目 | CN 创建新项目 / JP 新規プロジェクト作成 / EN Create new project")
    @Transactional
    public ProjectInitResult init(
            @McpToolParam(description = "项目名称", required = true) String name,
            @McpToolParam(description = "original | fanfic", required = true) String type,
            @McpToolParam(description = "额外元数据", required = false) Map<String, Object> meta) {

        Project p = new Project();
        p.setName(name);
        p.setType(type);
        p.setStatus("active");
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        if (meta != null) {
            try {
                p.setMeta(MAPPER.writeValueAsString(meta));
            } catch (Exception e) {
                log.warn("Failed to serialize meta JSON, storing as toString()", e);
                p.setMeta(meta.toString());
            }
        }
        p = projects.save(p);

        // 创建默认宇宙
        Universe defaultUniv = universeService.createDefaultUniverse(p);

        try {
            neo4j.query("CREATE (pr:Project {project_id: $pid, name: $name, type: $type, status: 'active'})")
                    .bind(p.getId().toString()).to("pid")
                    .bind(name).to("name")
                    .bind(type).to("type")
                    .run();
        } catch (Exception e) {
            log.warn("Neo4j project node creation failed for project {}", p.getId(), e);
        }

        return new ProjectInitResult(p.getId().toString(), name, type, "active", null);
    }


    /*
     * 归档项目 / アーカイブ / Archive
     *
     * CN 标记为归档，数据保留
     * JP アーカイブとしてマーク、データは保持
     * EN Mark as archived, data preserved
     */
    @McpTool(name = "project_archive", description = "归档项目（标记为归档，数据保留） | CN 归档项目 / JP プロジェクトをアーカイブ / EN Archive project")
    @Transactional
    public ProjectArchiveResult archive(
            @McpToolParam(description = "项目ID", required = true) String projectId) {

        Project p = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        p.setStatus("archived");
        p.setUpdatedAt(Instant.now());
        projects.save(p);

        try {
            neo4j.query("MATCH (pr:Project {project_id: $pid}) SET pr.status = 'archived'")
                    .bind(projectId).to("pid")
                    .run();
        } catch (Exception e) {
            log.warn("Neo4j project archive update failed for project {}", projectId, e);
        }

        return new ProjectArchiveResult(projectId, "archived");
    }


    /*
     * 删除项目 / 削除 / Delete
     *
     * CN 从 PG + Neo4j + Meilisearch 彻底清除
     * JP PG+Neo4j+Meilisearch から完全削除
     * EN Permanently delete from PG + Neo4j + Meilisearch
     */
    @McpTool(name = "project_delete", description = "物理删除项目——从 PG + Neo4j + Meilisearch 彻底清除所有数据 | CN 物理删除项目 / JP プロジェクトを完全削除 / EN Physically delete project")
    @Transactional
    public ProjectDeleteResult delete(
            @McpToolParam(description = "项目ID", required = true) String projectId) {

        UUID pid = UUID.fromString(projectId);
        if (!projects.existsById(pid)) {
            throw new IllegalArgumentException("Project not found: " + projectId);
        }

        int totalRows = 0;

        // 1. PG — delete child entities with direct project_id
        for (String table : PG_DIRECT_TABLES) {
            try {
                int n = em.createNativeQuery("DELETE FROM " + table + " WHERE project_id = ?1")
                        .setParameter(1, pid)
                        .executeUpdate();
                totalRows += n;
            } catch (Exception e) {
                log.warn("DELETE on {} failed (may not have project_id column): {}", table, e.getMessage());
            }
        }

        // 2. PG — delete child entities with indirect FK (no project_id)
        for (String[] spec : PG_JOIN_TABLES) {
            String table = spec[0], fkCol = spec[1], parentTable = spec[2];
            try {
                int n = em.createNativeQuery(
                                "DELETE FROM " + table + " WHERE " + fkCol + " IN " +
                                        "(SELECT id FROM " + parentTable + " WHERE project_id = ?1)")
                        .setParameter(1, pid)
                        .executeUpdate();
                totalRows += n;
            } catch (Exception e) {
                log.warn("DELETE on {} via {} failed: {}", table, parentTable, e.getMessage());
            }
        }

        // 2. PG — delete the project row itself (catch any remaining FK-skipped orphans)
        em.createNativeQuery("DELETE FROM projects WHERE id = ?1")
                .setParameter(1, pid)
                .executeUpdate();

        // 3. Neo4j — 删除该项目下的全部数据
        try {
            // Chapter 节点和 Project 节点之间没有直接关系，分两步删
            neo4j.query("MATCH (ch:Chapter {project_id: $pid}) DETACH DELETE ch")
                    .bind(projectId).to("pid")
                    .run();
            neo4j.query("MATCH (it:Item {project_id: $pid}) DETACH DELETE it")
                    .bind(projectId).to("pid")
                    .run();
            neo4j.query("MATCH (pr:Project {project_id: $pid}) DETACH DELETE pr")
                    .bind(projectId).to("pid")
                    .run();
        } catch (Exception e) {
            log.warn("Neo4j delete failed for project {}", projectId, e);
        }

        // 4. Meilisearch — delete documents for this project by filter
        try {
            meiliClient.post()
                    .uri("/indexes/novel_chapters/documents/delete-by-filter")
                    .bodyValue(Map.of("filter", List.of("project_id = '" + projectId + "'")))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(java.time.Duration.ofSeconds(5));
        } catch (Exception e) {
            log.warn("Meilisearch document delete failed for project {} (index may not exist): {}", projectId, e.getMessage());
        }

        return new ProjectDeleteResult(projectId, "deleted", totalRows);
    }

    @McpTool(name = "service_reset", description = "清空全部数据——清空 PG + Neo4j + Meilisearch 所有内容，恢复到初始状态 | CN 清空全部数据 / JP 全データをリセット / EN Reset all data")
    @Transactional
    public ServiceResetResult reset() {

        // 1. PG — truncate all tables in dependency-safe order, restart identity
        List<String> allTables = Arrays.asList(
                "character_voiceprints",
                "character_snapshots",
                "chapter_paragraphs",
                "chapter_versions",
                "deduction_logs",
                "timeline_events",
                "timelines",
                "timeline_links",
                "universe_relations",
                "universes",
                "foreshadowing_index",
                "canon_event_status",
                "character_relationships",
                "canon_relationships",
                "canon_events",
                "canon_characters",
                "canon_sources",
                "chapters",
                "character_profiles",
                "locations",
                "items",
                "projects"
        );
        for (String table : allTables) {
            try {
                em.createNativeQuery("DELETE FROM " + table).executeUpdate();
            } catch (Exception e) {
                log.warn("Failed to clear table {}: {}", table, e.getMessage());
            }
        }

        // 2. Neo4j — delete everything
        try {
            neo4j.query("MATCH (n) DETACH DELETE n").run();
        } catch (Exception e) {
            log.warn("Neo4j reset failed: {}", e.getMessage());
        }

        // 3. Meilisearch — delete all documents in novel_chapters index
        try {
            meiliClient.delete()
                    .uri("/indexes/novel_chapters")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(java.time.Duration.ofSeconds(5));
        } catch (Exception e) {
            log.warn("Meilisearch index delete failed (may not exist): {}", e.getMessage());
        }

        return new ServiceResetResult("ok", "所有数据已清空");
    }

    // ════════════════════════════════════════════════════════════════
    //  Export / Import — 数据和文件互为备份
    // ════════════════════════════════════════════════════════════════


    /*
     * 导出项目 / エクスポート / Export
     *
     * CN 导出全部数据为 JSON——备份/迁移用
     * JP 全データをJSONにエクスポート——バックアップ/移行用
     * EN Export all data as JSON — for backup/migration
     */
    @McpTool(name = "project_export", description = "导出项目全部数据为 JSON——用于备份、文件恢复或迁移 | CN 导出项目数据 / JP プロジェクトデータをエクスポート / EN Export project data")
    public ProjectExportResult exportData(
            @McpToolParam(description = "项目ID", required = true) String projectId) {

        UUID pid = UUID.fromString(projectId);
        if (!projects.existsById(pid)) {
            throw new IllegalArgumentException("Project not found: " + projectId);
        }
        String pidStr = pid.toString();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("_exported_at", Instant.now().toString());
        data.put("project_id", pidStr);

        // 1. 项目元数据
        Project p = projects.findById(pid).orElse(null);
        if (p != null) {
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("name", p.getName());
            pm.put("type", p.getType());
            pm.put("status", p.getStatus());
            pm.put("meta", safeParseJson(p.getMeta()));
            pm.put("created_at", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
            data.put("project", pm);
        }

        // 2. 章节
        List<Map<String, Object>> chaptersList = em.createNativeQuery(
                        "SELECT chapter_number, title, content, phase, status, word_count, summary, file_path FROM chapters WHERE project_id = ?1 ORDER BY chapter_number")
                .setParameter(1, pid)
                .getResultList()
                .stream().map(row -> {
                    Object[] r = (Object[]) row;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("number", r[0]);
                    m.put("title", r[1]);
                    m.put("content", r[2]);
                    m.put("phase", r[3]);
                    m.put("status", r[4]);
                    m.put("word_count", r[5]);
                    m.put("summary", r[6]);
                    m.put("file_path", r[7]);
                    return m;
                }).toList();
        data.put("chapters", chaptersList);

        // 3. 人物画像
        List<Map<String, Object>> charList = em.createNativeQuery(
                        "SELECT name, name_jp, name_en, bio, voice, type, traits::text, voice_meta::text, status::text, basic_info::text " +
                                "FROM character_profiles WHERE project_id = ?1 ORDER BY name")
                .setParameter(1, pid)
                .getResultList()
                .stream().map(row -> {
                    Object[] r = (Object[]) row;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", r[0]);
                    m.put("name_jp", r[1]);
                    m.put("name_en", r[2]);
                    m.put("bio", r[3]);
                    m.put("voice", r[4]);
                    m.put("type", r[5]);
                    tryParseJsonToField(m, "traits", (String) r[6]);
                    tryParseJsonToField(m, "voice_meta", (String) r[7]);
                    tryParseJsonToField(m, "status", (String) r[8]);
                    tryParseJsonToField(m, "basic_info", (String) r[9]);
                    return m;
                }).toList();
        data.put("characters", charList);

        // 4. 伏笔
        List<Map<String, Object>> foreshadows = em.createNativeQuery(
                        "SELECT code, description, f_type, planted_chapter, payoff_chapter, status FROM foreshadowing_index WHERE project_id = ?1 ORDER BY code")
                .setParameter(1, pid)
                .getResultList()
                .stream().map(row -> {
                    Object[] r = (Object[]) row;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("code", r[0]);
                    m.put("description", r[1]);
                    m.put("f_type", r[2]);
                    m.put("planted_chapter", r[3]);
                    m.put("payoff_chapter", r[4]);
                    m.put("status", r[5]);
                    return m;
                }).toList();
        data.put("foreshadowing", foreshadows);

        // 5. 时间线
        List<Map<String, Object>> timelines = em.createNativeQuery(
                        "SELECT id::text, name, type, description FROM timelines WHERE project_id = ?1 ORDER BY name")
                .setParameter(1, pid)
                .getResultList()
                .stream().map(row -> {
                    Object[] r = (Object[]) row;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r[0]);
                    m.put("name", r[1]);
                    m.put("type", r[2]);
                    m.put("description", r[3]);
                    return m;
                }).toList();
        data.put("timelines", timelines);

        // 6. 时间线事件
        List<Map<String, Object>> tlEvents = em.createNativeQuery(
                        "SELECT te.name, te.absolute_order, te.narrative_order, te.description, te.date_label, te.is_canon, tl.name as tl_name " +
                                "FROM timeline_events te JOIN timelines tl ON te.timeline_id = tl.id " +
                                "WHERE te.project_id = ?1 ORDER BY tl.name, te.absolute_order")
                .setParameter(1, pid)
                .getResultList()
                .stream().map(row -> {
                    Object[] r = (Object[]) row;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", r[0]);
                    m.put("absolute_order", r[1]);
                    m.put("narrative_order", r[2]);
                    m.put("description", r[3]);
                    m.put("date_label", r[4]);
                    m.put("is_canon", r[5]);
                    m.put("timeline", r[6]);
                    return m;
                }).toList();
        data.put("timeline_events", tlEvents);

        // 6b. 时间线关联
        List<Map<String, Object>> tlLinks = em.createNativeQuery(
                        "SELECT tl.id::text, ft.name as ft_name, tt.name as tt_name, tl.link_type, " +
                                "tl.from_absolute_order, tl.to_absolute_order, tl.description " +
                                "FROM timeline_links tl " +
                                "JOIN timelines ft ON tl.from_timeline_id = ft.id " +
                                "JOIN timelines tt ON tl.to_timeline_id = tt.id " +
                                "WHERE tl.project_id = ?1 ORDER BY ft.name, tt.name")
                .setParameter(1, pid)
                .getResultList()
                .stream().map(row -> {
                    Object[] r = (Object[]) row;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r[0]);
                    m.put("from_timeline", r[1]);
                    m.put("to_timeline", r[2]);
                    m.put("link_type", r[3]);
                    m.put("from_absolute_order", r[4]);
                    m.put("to_absolute_order", r[5]);
                    m.put("description", r[6]);
                    return m;
                }).toList();
        data.put("timeline_links", tlLinks);

        // 7. 地点档案
        List<Map<String, Object>> locationList = em.createNativeQuery(
                        "SELECT name, location_type, region, first_chapter, canon_description, actual_appearance, " +
                                "sensory_detail, narrative_function, change_log::text, current_status " +
                                "FROM locations WHERE project_id = ?1 ORDER BY name")
                .setParameter(1, pid)
                .getResultList()
                .stream().map(row -> {
                    Object[] r = (Object[]) row;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", r[0]);
                    m.put("location_type", r[1]);
                    m.put("region", r[2]);
                    m.put("first_chapter", r[3]);
                    m.put("canon_description", r[4]);
                    m.put("actual_appearance", r[5]);
                    m.put("sensory_detail", r[6]);
                    m.put("narrative_function", r[7]);
                    tryParseJsonToField(m, "change_log", (String) r[8]);
                    m.put("current_status", r[9]);
                    return m;
                }).toList();
        data.put("locations", locationList);

        // 7b. 物品档案
        List<Map<String, Object>> itemList = em.createNativeQuery(
                        "SELECT name, item_type, description, origin, significance, properties::text, " +
                                "current_holder, current_location, current_status, first_chapter, owner_history::text " +
                                "FROM items WHERE project_id = ?1 ORDER BY name")
                .setParameter(1, pid)
                .getResultList()
                .stream().map(row -> {
                    Object[] r = (Object[]) row;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", r[0]);
                    m.put("item_type", r[1]);
                    m.put("description", r[2]);
                    m.put("origin", r[3]);
                    m.put("significance", r[4]);
                    tryParseJsonToField(m, "properties", (String) r[5]);
                    m.put("current_holder", r[6]);
                    m.put("current_location", r[7]);
                    m.put("current_status", r[8]);
                    m.put("first_chapter", r[9]);
                    tryParseJsonToField(m, "owner_history", (String) r[10]);
                    return m;
                }).toList();
        data.put("items", itemList);

        // 8. 正典走向追踪
        List<Map<String, Object>> canonStatusList = em.createNativeQuery(
                        "SELECT ces.status, ces.actual_description, ces.occurred_in_chapter, ces.divergence_reason, " +
                                "ce.name as event_name, ce.id::text as event_id " +
                                "FROM canon_event_status ces JOIN canon_events ce ON ces.canon_event_id = ce.id " +
                                "WHERE ces.project_id = ?1 ORDER BY ce.name")
                .setParameter(1, pid)
                .getResultList()
                .stream().map(row -> {
                    Object[] r = (Object[]) row;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("status", r[0]);
                    m.put("actual_description", r[1]);
                    m.put("occurred_in_chapter", r[2]);
                    m.put("divergence_reason", r[3]);
                    m.put("event_name", r[4]);
                    m.put("event_id", r[5]);
                    return m;
                }).toList();
        data.put("canon_event_status", canonStatusList);

        String json;
        try {
            json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize export data", e);
        }

        return new ProjectExportResult(pidStr, "ok", json, chaptersList.size(), charList.size(), foreshadows.size(),
                timelines.size(), tlEvents.size(), locationList.size(), itemList.size());
    }


    /*
     * 导入项目 / インポート / Import
     *
     * CN 从 JSON 恢复全部数据
     * JP JSONから全データを復元
     * EN Restore all data from JSON
     */
    @McpTool(name = "project_import", description = "从 JSON 导入项目全部数据——用于从文件备份恢复数据库 | CN 导入项目数据 / JP プロジェクトデータをインポート / EN Import project data")
    @Transactional
    public ProjectImportResult importData(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "导出 JSON（project_export 的输出）", required = true) String jsonData) {

        UUID pid = UUID.fromString(projectId);
        if (!projects.existsById(pid)) {
            throw new IllegalArgumentException("Project not found: " + projectId);
        }

        Map<String, Object> data;
        try {
            data = MAPPER.readValue(jsonData, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON data: " + e.getMessage());
        }

        int ch = 0, cp = 0, fs = 0, tl = 0, te = 0, lc = 0;

        // 1. 章节
        List<Map<String, Object>> chapters = safeGetList(data, "chapters");
        for (Map<String, Object> c : chapters) {
            em.createNativeQuery(
                            "INSERT INTO chapters (id, project_id, chapter_number, title, content, phase, status, word_count, summary, file_path, created_at, updated_at) " +
                                    "VALUES (gen_random_uuid(), ?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, now(), now()) " +
                                    "ON CONFLICT (project_id, chapter_number) DO UPDATE SET title=EXCLUDED.title, content=EXCLUDED.content, phase=EXCLUDED.phase, status=EXCLUDED.status, word_count=EXCLUDED.word_count, summary=EXCLUDED.summary, updated_at=now()")
                    .setParameter(1, pid)
                    .setParameter(2, c.get("number"))
                    .setParameter(3, c.get("title"))
                    .setParameter(4, c.get("content"))
                    .setParameter(5, c.get("phase"))
                    .setParameter(6, c.get("status"))
                    .setParameter(7, c.get("word_count"))
                    .setParameter(8, c.get("summary"))
                    .setParameter(9, c.get("file_path"))
                    .executeUpdate();
            ch++;
        }

        // 2. 人物画像
        List<Map<String, Object>> characters = safeGetList(data, "characters");
        for (Map<String, Object> c : characters) {
            String chName = (String) c.get("name");
            if (chName == null || chName.isBlank()) continue;
            em.createNativeQuery(
                            "INSERT INTO character_profiles (id, project_id, name, bio, voice, type, traits, voice_meta, status, basic_info, created_at, updated_at) " +
                                    "VALUES (gen_random_uuid(), :pid, :name, :bio, :voice, :type, CAST(:traits AS jsonb), CAST(:vmeta AS jsonb), CAST(:status AS jsonb), CAST(:binfo AS jsonb), now(), now()) " +
                                    "ON CONFLICT (project_id, name) DO UPDATE SET bio=EXCLUDED.bio, voice=EXCLUDED.voice, traits=EXCLUDED.traits, updated_at=now()")
                    .setParameter("pid", pid)
                    .setParameter("name", chName)
                    .setParameter("bio", c.get("bio"))
                    .setParameter("voice", c.get("voice"))
                    .setParameter("type", c.get("type"))
                    .setParameter("traits", safeToJsonString(c.get("traits")))
                    .setParameter("vmeta", safeToJsonString(c.get("voice_meta")))
                    .setParameter("status", safeToJsonString(c.get("status_meta")))
                    .setParameter("binfo", safeToJsonString(c.get("basic_info")))
                    .executeUpdate();
            cp++;
        }

        // 3. 伏笔
        List<Map<String, Object>> foreshadows = safeGetList(data, "foreshadowing");
        for (Map<String, Object> f : foreshadows) {
            String code = (String) f.get("code");
            if (code == null) continue;
            em.createNativeQuery(
                            "INSERT INTO foreshadowing_index (id, project_id, code, description, f_type, planted_chapter, payoff_chapter, status, created_at, updated_at) " +
                                    "VALUES (gen_random_uuid(), ?1, ?2, ?3, ?4, ?5, ?6, ?7, now(), now()) " +
                                    "ON CONFLICT (project_id, code) DO UPDATE SET description=EXCLUDED.description, status=EXCLUDED.status, payoff_chapter=EXCLUDED.payoff_chapter, updated_at=now()")
                    .setParameter(1, pid)
                    .setParameter(2, code)
                    .setParameter(3, f.get("description"))
                    .setParameter(4, f.get("f_type"))
                    .setParameter(5, f.get("planted_chapter"))
                    .setParameter(6, f.get("payoff_chapter"))
                    .setParameter(7, f.get("status"))
                    .executeUpdate();
            fs++;
        }

        // 4. 时间线
        List<Map<String, Object>> timelines = safeGetList(data, "timelines");
        for (Map<String, Object> t : timelines) {
            String tlName = (String) t.get("name");
            if (tlName == null) continue;
            em.createNativeQuery(
                            "INSERT INTO timelines (id, project_id, name, type, description, created_at) " +
                                    "VALUES (gen_random_uuid(), ?1, ?2, ?3, ?4, now()) " +
                                    "ON CONFLICT DO NOTHING")
                    .setParameter(1, pid)
                    .setParameter(2, tlName)
                    .setParameter(3, t.get("type"))
                    .setParameter(4, t.get("description"))
                    .executeUpdate();
            tl++;
        }

        // 5. 时间线事件
        List<Map<String, Object>> tlEvents = safeGetList(data, "timeline_events");
        for (Map<String, Object> e : tlEvents) {
            String tlName = (String) e.get("timeline");
            if (tlName == null) continue;
            // resolve timeline id by name
            List<?> tlRows = em.createNativeQuery(
                            "SELECT id::text FROM timelines WHERE project_id = ?1 AND name = ?2 LIMIT 1")
                    .setParameter(1, pid).setParameter(2, tlName).getResultList();
            if (tlRows.isEmpty()) continue;
            Object tlId = tlRows.get(0);
            em.createNativeQuery(
                            "INSERT INTO timeline_events (id, project_id, timeline_id, name, date_label, absolute_order, narrative_order, description, is_canon, created_at) " +
                                    "VALUES (gen_random_uuid(), :p1, :p2::uuid, :p3, :p4, :p5, :p6, :p7, :p8, now()) " +
                                    "ON CONFLICT DO NOTHING")
                    .setParameter("p1", pid)
                    .setParameter("p2", tlId)
                    .setParameter("p3", e.get("name"))
                    .setParameter("p4", e.get("date_label"))
                    .setParameter("p5", e.get("absolute_order"))
                    .setParameter("p6", e.get("narrative_order"))
                    .setParameter("p7", e.get("description"))
                    .setParameter("p8", e.get("is_canon"))
                    .executeUpdate();
            te++;
        }

        // 6b. 时间线关联
        List<Map<String, Object>> tlLinks = safeGetList(data, "timeline_links");
        for (Map<String, Object> l : tlLinks) {
            String ftName = (String) l.get("from_timeline");
            String ttName = (String) l.get("to_timeline");
            if (ftName == null || ttName == null) continue;
            List<?> ftRows = em.createNativeQuery(
                            "SELECT id::text FROM timelines WHERE project_id = ?1 AND name = ?2 LIMIT 1")
                    .setParameter(1, pid).setParameter(2, ftName).getResultList();
            List<?> ttRows = em.createNativeQuery(
                            "SELECT id::text FROM timelines WHERE project_id = ?1 AND name = ?2 LIMIT 1")
                    .setParameter(1, pid).setParameter(2, ttName).getResultList();
            if (ftRows.isEmpty() || ttRows.isEmpty()) continue;
            em.createNativeQuery(
                            "INSERT INTO timeline_links (id, project_id, from_timeline_id, to_timeline_id, link_type, from_absolute_order, to_absolute_order, description, created_at) " +
                                    "VALUES (gen_random_uuid(), :p1, :p2::uuid, :p3::uuid, :p4, :p5, :p6, :p7, now()) " +
                                    "ON CONFLICT DO NOTHING")
                    .setParameter("p1", pid)
                    .setParameter("p2", ftRows.get(0))
                    .setParameter("p3", ttRows.get(0))
                    .setParameter("p4", l.get("link_type"))
                    .setParameter("p5", l.get("from_absolute_order"))
                    .setParameter("p6", l.get("to_absolute_order"))
                    .setParameter("p7", l.get("description"))
                    .executeUpdate();
        }

        // 7. 地点档案
        List<Map<String, Object>> locs = safeGetList(data, "locations");
        for (Map<String, Object> l : locs) {
            String locName = (String) l.get("name");
            if (locName == null) continue;
            String locChangeLog = safeToJsonString(l.get("change_log"));
            em.createNativeQuery(
                            "INSERT INTO locations (id, project_id, name, location_type, region, first_chapter, " +
                                    "canon_description, actual_appearance, sensory_detail, narrative_function, " +
                                    "change_log, current_status, created_at, updated_at) " +
                                    "VALUES (gen_random_uuid(), :pid, :name, :type, :region, :fc, " +
                                    ":cd, :aa, :sd, :nf, CAST(:cl AS jsonb), :cs, now(), now()) " +
                                    "ON CONFLICT (project_id, name) DO UPDATE SET " +
                                    "canon_description=EXCLUDED.canon_description, actual_appearance=EXCLUDED.actual_appearance, " +
                                    "change_log=EXCLUDED.change_log, current_status=EXCLUDED.current_status, updated_at=now()")
                    .setParameter("pid", pid)
                    .setParameter("name", locName)
                    .setParameter("type", l.get("location_type"))
                    .setParameter("region", l.get("region"))
                    .setParameter("fc", l.get("first_chapter"))
                    .setParameter("cd", l.get("canon_description"))
                    .setParameter("aa", l.get("actual_appearance"))
                    .setParameter("sd", l.get("sensory_detail"))
                    .setParameter("nf", l.get("narrative_function"))
                    .setParameter("cl", locChangeLog)
                    .setParameter("cs", l.get("current_status"))
                    .executeUpdate();
            lc++;
        }

        // 7b. 物品档案
        int it = 0;
        List<Map<String, Object>> itemListData = safeGetList(data, "items");
        for (Map<String, Object> im : itemListData) {
            String itemName = (String) im.get("name");
            if (itemName == null) continue;
            String propsJson = safeToJsonString(im.get("properties"));
            String histJson = safeToJsonString(im.get("owner_history"));
            em.createNativeQuery(
                            "INSERT INTO items (id, project_id, name, item_type, description, origin, significance, " +
                                    "properties, current_holder, current_location, current_status, first_chapter, " +
                                    "owner_history, created_at, updated_at) " +
                                    "VALUES (gen_random_uuid(), :pid, :name, :type, :desc, :origin, :sig, " +
                                    "CAST(:props AS jsonb), :holder, :loc, :status, :fc, CAST(:hist AS jsonb), now(), now()) " +
                                    "ON CONFLICT (project_id, name) DO UPDATE SET " +
                                    "description=EXCLUDED.description, origin=EXCLUDED.origin, " +
                                    "current_holder=EXCLUDED.current_holder, current_location=EXCLUDED.current_location, " +
                                    "current_status=EXCLUDED.current_status, owner_history=EXCLUDED.owner_history, " +
                                    "properties=EXCLUDED.properties, updated_at=now()")
                    .setParameter("pid", pid)
                    .setParameter("name", itemName)
                    .setParameter("type", im.get("item_type"))
                    .setParameter("desc", im.get("description"))
                    .setParameter("origin", im.get("origin"))
                    .setParameter("sig", im.get("significance"))
                    .setParameter("props", propsJson)
                    .setParameter("holder", im.get("current_holder"))
                    .setParameter("loc", im.get("current_location"))
                    .setParameter("status", im.get("current_status"))
                    .setParameter("fc", im.get("first_chapter"))
                    .setParameter("hist", histJson)
                    .executeUpdate();
            it++;
        }

        // 8. 正典走向追踪
        List<Map<String, Object>> cesList = safeGetList(data, "canon_event_status");
        for (Map<String, Object> ces : cesList) {
            String eventId = (String) ces.get("event_id");
            String eventName = (String) ces.get("event_name");
            if (eventId == null && eventName != null) {
                // resolve by name
                List<?> rows = em.createNativeQuery(
                                "SELECT id::text FROM canon_events WHERE project_id = :p AND name = :n LIMIT 1")
                        .setParameter("p", pid).setParameter("n", eventName).getResultList();
                if (!rows.isEmpty()) eventId = (String) rows.get(0);
            }
            if (eventId == null) continue;
            em.createNativeQuery(
                            "INSERT INTO canon_event_status (id, project_id, canon_event_id, status, actual_description, occurred_in_chapter, divergence_reason, created_at, updated_at) " +
                                    "VALUES (gen_random_uuid(), :p1, :p2::uuid, :p3, :p4, :p5, :p6, now(), now()) " +
                                    "ON CONFLICT (project_id, canon_event_id) DO UPDATE SET " +
                                    "status=EXCLUDED.status, actual_description=EXCLUDED.actual_description, " +
                                    "occurred_in_chapter=EXCLUDED.occurred_in_chapter, divergence_reason=EXCLUDED.divergence_reason, updated_at=now()")
                    .setParameter("p1", pid)
                    .setParameter("p2", eventId)
                    .setParameter("p3", ces.get("status"))
                    .setParameter("p4", ces.get("actual_description"))
                    .setParameter("p5", ces.get("occurred_in_chapter"))
                    .setParameter("p6", ces.get("divergence_reason"))
                    .executeUpdate();
        }

        // 9. Neo4j — 更新项目节点
        try {
            neo4j.query("MERGE (pr:Project {project_id: $pid}) SET pr.updatedAt = datetime()")
                    .bind(projectId).to("pid").run();
        } catch (Exception ex) {
            log.warn("Neo4j import update failed for project {}", projectId, ex);
        }

        return new ProjectImportResult(projectId, "ok", ch, cp, fs, tl, te, lc, it);
    }

    // ── 辅助方法 ──

    private Map<String, Object> safeParseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return null;
        }
    }

    private void tryParseJsonToField(Map<String, Object> target, String key, String json) {
        if (json == null || json.isBlank()) return;
        try {
            target.put(key, MAPPER.readValue(json, Object.class));
        } catch (Exception e) {
            target.put(key, json);
        }
    }

    private String safeToJsonString(Object obj) {
        if (obj == null) return null;
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> safeGetList(Map<String, Object> data, String key) {
        Object val = data.get(key);
        if (val instanceof List) return (List<Map<String, Object>>) val;
        return List.of();
    }

    public record ProjectInitResult(String projectId, String name, String type, String status, String note) {
    }

    public record ProjectArchiveResult(String projectId, String status) {
    }

    public record ProjectDeleteResult(String projectId, String status, int deletedRows) {
    }

    public record ServiceResetResult(String status, String message) {
    }

    public record ProjectExportResult(String projectId, String status, String json, int chapters, int characters,
                                      int foreshadowing, int timelines, int timelineEvents, int locations, int items) {
    }

    public record ProjectImportResult(String projectId, String status, int chapters, int characters, int foreshadowing,
                                      int timelines, int timelineEvents, int locations, int items) {
    }
}
