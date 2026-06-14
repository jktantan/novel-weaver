package com.novelweaver.service;

/*
 * Universe Service / 宇宙管理 / 宇宙管理
 *
 * CN 宇宙创建、列表、关联
 * JP 宇宙作成、一覧、関連付け
 * EN Universe create, list, link
 */

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
import org.springframework.data.neo4j.core.Neo4jClient;
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
    private final Neo4jClient neo4j;

    public UniverseService(UniverseRepository universes, UniverseRelationRepository relations,
                           ProjectRepository projects, Neo4jClient neo4j) {
        this.universes = universes;
        this.relations = relations;
        this.projects = projects;
        this.neo4j = neo4j;
    }


    /*
     * 创建宇宙 / 作成 / Create
     *
     * CN 创建宇宙（原创/同人/融合）
     * JP 宇宙を作成（オリジナル/二次創作/クロスオーバー）
     * EN Create a universe (original/fanfic/crossover)
     */
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

        // Neo4j: create :Universe node
        try {
            neo4j.query("""
                            MERGE (u:Universe {project_id: $pid, name: $name})
                            SET u.type = $type
                            """)
                    .bind(projectId).to("pid")
                    .bind(name).to("name")
                    .bind(type).to("type")
                    .run();
        } catch (Exception e) {
            log.warn("Neo4j universe node creation failed for {}/{}", projectId, name, e);
        }

        return new UniverseCreateResult("ok", u.getId().toString(), name, type);
    }


    /*
     * 宇宙列表 / 一覧 / List
     *
     * CN 列出项目所有宇宙
     * JP プロジェクトの全宇宙を一覧
     * EN List all universes in project
     */
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


    /*
     * 关联宇宙 / 関連付け / Link
     *
     * CN 连接两个宇宙（平行/衍生/跨界）
     * JP 2つの宇宙を関連付け（並行/派生/クロス）
     * EN Link two universes (parallel/derived/crossover)
     */
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

        // Neo4j: create relationship between :Universe nodes
        try {
            neo4j.query("""
                            MATCH (a:Universe {project_id: $pid, name: $fromName})
                            MATCH (b:Universe {project_id: $pid, name: $toName})
                            MERGE (a)-[:RELATED_TO {type: $relType}]->(b)
                            """)
                    .bind(projectId).to("pid")
                    .bind(from.getName()).to("fromName")
                    .bind(to.getName()).to("toName")
                    .bind(relationType).to("relType")
                    .run();
        } catch (Exception e) {
            log.warn("Neo4j universe link failed for {}/{}->{}", projectId, from.getName(), to.getName(), e);
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
