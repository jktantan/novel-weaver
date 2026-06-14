package com.novelweaver.service;

/*
 * Universe Service / 宇宙管理 / 宇宙管理
 *
 * CN 宇宙创建、列表、关联 — 图部分使用 ArcadeDB（物理租户，无需 project_id 过滤）
 * JP 宇宙作成、一覧、関連付け — グラフ部分は ArcadeDB を使用
 * EN Universe create, list, link — graph via ArcadeDB (physical tenant)
 */

import com.arcadedb.remote.RemoteDatabase;
import com.novelweaver.config.ArcadeDBManager;
import com.novelweaver.model.Project;
import com.novelweaver.model.Universe;
import com.novelweaver.model.UniverseRelation;
import com.novelweaver.repository.ProjectRepository;
import com.novelweaver.repository.UniverseRelationRepository;
import com.novelweaver.repository.UniverseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Component
public class UniverseService {

    private static final Logger log = LoggerFactory.getLogger(UniverseService.class);

    private final UniverseRepository universes;
    private final UniverseRelationRepository relations;
    private final ProjectRepository projects;
    private final ArcadeDBManager arcadeDB;

    public UniverseService(UniverseRepository universes, UniverseRelationRepository relations,
                           ProjectRepository projects, ArcadeDBManager arcadeDB) {
        this.universes = universes;
        this.relations = relations;
        this.projects = projects;
        this.arcadeDB = arcadeDB;
    }


    @McpTool(name = "universe_create", description = "创建宇宙——一个项目可有多个宇宙（原创/同人/融合），每个宇宙可有自己的时间线、人物画像、正典 | CN 创建宇宙 / JP 新しい宇宙を作成 / EN Create universe")
    @Transactional
    public UniverseCreateResult create(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "宇宙名", required = true) String name,
            @McpToolParam(description = "类型 original(原创) / fanfic(同人) / crossover(融合)", required = true) String type,
            @McpToolParam(description = "描述", required = false) String description) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (universes.findByProjectAndName(proj, name).isPresent()) {
            throw new IllegalArgumentException("Universe already exists: " + name);
        }

        Universe u = new Universe();
        u.setProject(proj);
        u.setName(name);
        u.setType(type);
        u.setDescription(description != null ? description : "");
        u.setCreatedAt(Instant.now());
        u.setUpdatedAt(Instant.now());
        universes.save(u);

        // ArcadeDB: create :Universe node (no project_id filter — physical tenant)
        try (RemoteDatabase db = arcadeDB.open(projectId)) {
            db.command("cypher", "MERGE (u:Universe {name: $name}) SET u.type = $type",
                    Map.of("name", name, "type", type));
        } catch (Exception e) {
            log.warn("ArcadeDB universe node creation failed for {}/{}", projectId, name, e);
        }

        return new UniverseCreateResult("ok", u.getId().toString(), name, type);
    }


    @McpTool(name = "universe_list", description = "列出项目的所有宇宙 | CN 列出所有宇宙 / JP 全宇宙を一覧 / EN List all universes")
    public UniverseListResult list(
            @McpToolParam(description = "项目ID", required = true) String projectId) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        List<Universe> all = universes.findByProject(proj);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Universe u : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId().toString());
            m.put("name", u.getName());
            m.put("type", u.getType());
            m.put("description", u.getDescription());
            items.add(m);
        }
        return new UniverseListResult("ok", items);
    }


    @McpTool(name = "universe_link", description = "连接两个宇宙——表示平行、衍生或跨界关系 | CN 连接两个宇宙 / JP 2つの宇宙を関連付け / EN Link two universes")
    @Transactional
    public UniverseLinkResult link(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "源宇宙ID", required = true) String fromUniverseId,
            @McpToolParam(description = "目标宇宙ID", required = true) String toUniverseId,
            @McpToolParam(description = "关系类型 parallel_to(平行)/derived_from(衍生)/crosses_over_with(跨界)", required = true) String relationType,
            @McpToolParam(description = "描述", required = false) String description) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        Universe from = universes.findById(UUID.fromString(fromUniverseId))
                .orElseThrow(() -> new IllegalArgumentException("Universe not found: " + fromUniverseId));
        Universe to = universes.findById(UUID.fromString(toUniverseId))
                .orElseThrow(() -> new IllegalArgumentException("Universe not found: " + toUniverseId));

        UniverseRelation r = new UniverseRelation();
        r.setProject(proj);
        r.setFromUniverse(from);
        r.setToUniverse(to);
        r.setRelationType(relationType);
        r.setDescription(description != null ? description : "");
        r.setCreatedAt(Instant.now());
        relations.save(r);

        try (RemoteDatabase db = arcadeDB.open(projectId)) {
            db.command("cypher", """
                            MATCH (a:Universe {name: $fromName})
                            MATCH (b:Universe {name: $toName})
                            MERGE (a)-[:RELATED_TO {type: $relType}]->(b)
                            """,
                    Map.of("fromName", from.getName(), "toName", to.getName(), "relType", relationType));
        } catch (Exception e) {
            log.warn("ArcadeDB universe link failed for {}/{}->{}", projectId, from.getName(), to.getName(), e);
        }

        return new UniverseLinkResult("ok", r.getId().toString(),
                from.getName(), to.getName(), relationType);
    }

    public Universe createDefaultUniverse(Project proj) {
        Universe u = new Universe();
        u.setProject(proj);
        u.setName("主宇宙");
        u.setType("original");
        u.setDescription("默认宇宙");
        u.setCreatedAt(Instant.now());
        u.setUpdatedAt(Instant.now());
        return universes.save(u);
    }

    public record UniverseCreateResult(String status, String universeId, String name, String type) {
    }

    public record UniverseListResult(String status, List<Map<String, Object>> universes) {
    }

    public record UniverseLinkResult(String status, String linkId, String fromUniverse, String toUniverse,
                                     String relationType) {
    }
}
