package com.novelweaver.service;

/*
 * Item Service / 物品管理 / アイテム管理
 *
 * CN 物品注册、变更、查询
 * JP アイテム登録、変更、照会
 * EN Item register, update, query
 */

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelweaver.model.Item;
import com.novelweaver.model.Project;
import com.novelweaver.repository.ItemRepository;
import com.novelweaver.repository.ProjectRepository;
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
public class ItemService {

    private static final Logger log = LoggerFactory.getLogger(ItemService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ItemRepository items;
    private final ProjectRepository projects;
    private final Neo4jClient neo4j;

    public ItemService(ItemRepository items, ProjectRepository projects, Neo4jClient neo4j) {
        this.items = items;
        this.projects = projects;
        this.neo4j = neo4j;
    }


    /*
     * 注册物品 / 登録 / Register
     *
     * CN 注册新物品，记录来源、用途、初始持有者
     * JP 新しいアイテムを登録、来歴・用途・初期所持者を記録
     * EN Register new item with origin, usage, initial holder
     */
    @McpTool(name = "item_register", description = "注册新物品——记录名称、类型、描述、来源、用途、初始持有者 | CN 注册新物品 / JP アイテムを登録 / EN Register new item")
    @Transactional
    public ItemRegisterResult register(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "物品名", required = true) String name,
            @McpToolParam(description = "类型（武器/信物/神器/日常物品/其他）", required = false) String itemType,
            @McpToolParam(description = "外观/功能描述", required = false) String description,
            @McpToolParam(description = "来源：谁造的/哪发现的", required = false) String origin,
            @McpToolParam(description = "剧情意义/用途", required = false) String significance,
            @McpToolParam(description = "自定义属性 JSON（如魔法能力）", required = false) Map<String, Object> properties,
            @McpToolParam(description = "当前持有者", required = false) String currentHolder,
            @McpToolParam(description = "当前位置", required = false) String currentLocation,
            @McpToolParam(description = "当前状态（正常/损坏/遗失等）", required = false) String currentStatus,
            @McpToolParam(description = "首次出现章节", required = false) Integer firstChapter) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (items.findByProjectAndName(proj, name).isPresent()) {
            throw new IllegalArgumentException("Item already exists: " + name);
        }

        Item item = new Item();
        item.setProject(proj);
        item.setName(name);
        item.setItemType(itemType != null ? itemType : "");
        item.setDescription(description != null ? description : "");
        item.setOrigin(origin != null ? origin : "");
        item.setSignificance(significance != null ? significance : "");
        try {
            item.setProperties(properties != null ? MAPPER.writeValueAsString(properties) : "{}");
        } catch (Exception e) {
            item.setProperties("{}");
        }
        item.setCurrentHolder(currentHolder != null ? currentHolder : "");
        item.setCurrentLocation(currentLocation != null ? currentLocation : "");
        item.setCurrentStatus(currentStatus != null ? currentStatus : "正常");
        item.setFirstChapter(firstChapter != null ? firstChapter : 1);
        item.setOwnerHistory("[]");
        item.setCreatedAt(Instant.now());
        item.setUpdatedAt(Instant.now());
        items.save(item);

        // Neo4j: create :Item node
        try {
            neo4j.query("""
                            MERGE (it:Item {project_id: $pid, name: $name})
                            SET it.itemType = $type, it.significance = $sig
                            """)
                    .bind(projectId).to("pid")
                    .bind(name).to("name")
                    .bind(item.getItemType()).to("type")
                    .bind(item.getSignificance()).to("sig")
                    .run();

            // Link to current holder if specified
            if (currentHolder != null && !currentHolder.isBlank()) {
                neo4j.query("""
                                MATCH (it:Item {project_id: $pid, name: $name})
                                MERGE (c:Character {project_id: $pid, name: $holder})
                                MERGE (c)-[:OWNS]->(it)
                                """)
                        .bind(projectId).to("pid")
                        .bind(name).to("name")
                        .bind(currentHolder).to("holder")
                        .run();
            }

            // Link to current location if specified
            if (currentLocation != null && !currentLocation.isBlank()) {
                neo4j.query("""
                                MATCH (it:Item {project_id: $pid, name: $name})
                                MERGE (loc:Location {project_id: $pid, name: $locName})
                                MERGE (loc)-[:CONTAINS]->(it)
                                """)
                        .bind(projectId).to("pid")
                        .bind(name).to("name")
                        .bind(currentLocation).to("locName")
                        .run();
            }
        } catch (Exception e) {
            log.warn("Neo4j item node creation failed for item {}/{}", projectId, name, e);
        }

        return new ItemRegisterResult("ok", item.getId().toString(), name);
    }


    /*
     * 更新物品 / 更新 / Update
     *
     * CN 更新物品状态/持有者/位置，自动追加归属变更历史
     * JP アイテムの状態/所持者/位置を更新、所有履歴を自動追加
     * EN Update item status/holder/location, auto-append owner history
     */
    @McpTool(name = "item_update", description = "更新物品——变更持有者/位置/状态，自动记录归属历史 | CN 更新物品 / JP アイテムを更新 / EN Update item")
    @Transactional
    public ItemUpdateResult update(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "物品名", required = true) String name,
            @McpToolParam(description = "变更发生的章节号", required = true) Integer chapter,
            @McpToolParam(description = "触发事件（如赠予/遗失/发现）", required = true) String event,
            @McpToolParam(description = "新持有者（无则留空）", required = false) String newHolder,
            @McpToolParam(description = "新位置（无则留空）", required = false) String newLocation,
            @McpToolParam(description = "新状态（正常/损坏/遗失/销毁等）", required = false) String newStatus,
            @McpToolParam(description = "变更描述", required = false) String changeDescription) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        Item item = items.findByProjectAndName(proj, name)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + name));

        String oldHolder = item.getCurrentHolder();

        // 更新持有者/位置/状态
        if (newHolder != null && !newHolder.isBlank()) {
            item.setCurrentHolder(newHolder);
        }
        if (newLocation != null && !newLocation.isBlank()) {
            item.setCurrentLocation(newLocation);
        }
        if (newStatus != null && !newStatus.isBlank()) {
            item.setCurrentStatus(newStatus);
        }

        // 追加归属变更记录
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("chapter", chapter);
        entry.put("event", event);
        entry.put("from", oldHolder != null ? oldHolder : "");
        entry.put("to", newHolder != null ? newHolder : item.getCurrentHolder());
        entry.put("change", changeDescription != null ? changeDescription : "");

        try {
            List<Map<String, Object>> history;
            String existing = item.getOwnerHistory();
            if (existing != null && !existing.isBlank() && !"[]".equals(existing.trim())) {
                history = MAPPER.readValue(existing, new TypeReference<List<Map<String, Object>>>() {
                });
            } else {
                history = new ArrayList<>();
            }
            history.add(entry);
            item.setOwnerHistory(MAPPER.writeValueAsString(history));
        } catch (Exception e) {
            throw new RuntimeException("Failed to update owner history", e);
        }

        item.setUpdatedAt(Instant.now());
        items.save(item);

        // Neo4j: update relationships
        try {
            if (newHolder != null && !newHolder.isBlank() && !newHolder.equals(oldHolder)) {
                // Remove old OWNS relationships
                if (oldHolder != null && !oldHolder.isBlank()) {
                    neo4j.query("""
                                    MATCH (:Character {project_id: $pid, name: $old})-[r:OWNS]->(:Item {project_id: $pid, name: $name})
                                    DELETE r
                                    """)
                            .bind(projectId).to("pid")
                            .bind(oldHolder).to("old")
                            .bind(name).to("name")
                            .run();
                }
                // Create new OWNS
                neo4j.query("""
                                MATCH (it:Item {project_id: $pid, name: $name})
                                MERGE (c:Character {project_id: $pid, name: $holder})
                                MERGE (c)-[:OWNS]->(it)
                                """)
                        .bind(projectId).to("pid")
                        .bind(name).to("name")
                        .bind(newHolder).to("holder")
                        .run();
            }
        } catch (Exception e) {
            log.warn("Neo4j item update failed for item {}/{}", projectId, name, e);
        }

        return new ItemUpdateResult("ok", name, chapter, event, newHolder, newStatus);
    }


    /*
     * 查询物品 / 照会 / Query
     *
     * CN 查询物品详情 + 归属历史 + 当前状态
     * JP アイテム詳細+所有履歴+現在状態を照会
     * EN Query item details + owner history + current status
     */
    @McpTool(name = "item_query", description = "查询物品详情——返回基本信息 + 归属变更历史 + 当前状态 | CN 查询物品 / JP アイテムを照会 / EN Query item")
    public ItemQueryResult query(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "物品名", required = true) String name) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        Item item = items.findByProjectAndName(proj, name)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + name));

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("name", item.getName());
        profile.put("type", item.getItemType());
        profile.put("description", item.getDescription());
        profile.put("origin", item.getOrigin());
        profile.put("significance", item.getSignificance());
        try {
            profile.put("properties", item.getProperties() != null
                    ? MAPPER.readValue(item.getProperties(), Object.class) : Map.of());
        } catch (Exception e) {
            profile.put("properties", item.getProperties());
        }
        profile.put("current_holder", item.getCurrentHolder());
        profile.put("current_location", item.getCurrentLocation());
        profile.put("current_status", item.getCurrentStatus());
        profile.put("first_chapter", item.getFirstChapter());

        List<Map<String, Object>> history = new ArrayList<>();
        String historyJson = item.getOwnerHistory();
        if (historyJson != null && !historyJson.isBlank() && !"[]".equals(historyJson.trim())) {
            try {
                history = MAPPER.readValue(historyJson, new TypeReference<List<Map<String, Object>>>() {
                });
            } catch (Exception e) {
                log.warn("Failed to parse owner_history for item {}", name, e);
            }
        }

        // Neo4j: also return related characters/locations
        List<Map<String, Object>> relations = new ArrayList<>();
        try {
            var rows = neo4j.query("""
                            MATCH (it:Item {project_id: $pid, name: $name})-[r]-(n)
                            RETURN type(r) AS relType, labels(n) AS nodeLabels, n.name AS nodeName
                            LIMIT 20
                            """)
                    .bind(projectId).to("pid")
                    .bind(name).to("name")
                    .fetch()
                    .all();
            for (var row : rows) {
                Map<String, Object> rel = new LinkedHashMap<>();
                rel.put("type", row.get("relType"));
                rel.put("node", row.get("nodeName"));
                rel.put("nodeType", row.get("nodeLabels"));
                relations.add(rel);
            }
        } catch (Exception e) {
            log.warn("Neo4j item relations query failed for {}/{}", projectId, name, e);
        }

        return new ItemQueryResult("ok", name, item.getCurrentStatus(), profile, history, relations);
    }

    // ── result records ──

    public record ItemRegisterResult(String status, String itemId, String name) {
    }

    public record ItemUpdateResult(String status, String name, int chapter, String event,
                                   String newHolder, String newStatus) {
    }

    public record ItemQueryResult(String status, String name, String currentStatus,
                                  Map<String, Object> profile,
                                  List<Map<String, Object>> ownerHistory,
                                  List<Map<String, Object>> relations) {
    }
}
