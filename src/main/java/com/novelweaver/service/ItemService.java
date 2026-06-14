package com.novelweaver.service;

import com.arcadedb.remote.RemoteDatabase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelweaver.config.ArcadeDBManager;
import com.novelweaver.model.Item;
import com.novelweaver.model.Project;
import com.novelweaver.repository.ItemRepository;
import com.novelweaver.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Component
public class ItemService {

    private static final Logger log = LoggerFactory.getLogger(ItemService.class);

    private final ItemRepository items;
    private final ProjectRepository projects;
    private final ArcadeDBManager arcadeDB;
    private final ObjectMapper mapper;

    public ItemService(ItemRepository items, ProjectRepository projects,
                       ArcadeDBManager arcadeDB, ObjectMapper mapper) {
        this.items = items;
        this.projects = projects;
        this.arcadeDB = arcadeDB;
        this.mapper = mapper;
    }

    private String identityToString(Map<String, Object> identity) {
        if (identity == null || identity.isEmpty()) return "{}";
        try {
            return mapper.writeValueAsString(identity);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Item resolveItem(Project proj, String name, Map<String, Object> identity) {
        String idJson = identityToString(identity);
        if (!idJson.equals("{}")) return items.findByProjectAndNameAndIdentity(proj, name, idJson).orElse(null);
        List<Item> matches = items.findByProjectAndName(proj, name);
        if (matches.isEmpty()) return null;
        if (matches.size() == 1) return matches.get(0);
        throw new IllegalArgumentException("Multiple items named '" + name + "' found. Specify identity.");
    }

    @McpTool(name = "item_register", description = "注册新物品 | CN 注册新物品 / JP アイテムを登録 / EN Register new item")
    @Transactional
    public ItemRegisterResult register(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "物品名", required = true) String name,
            @McpToolParam(description = "类型", required = false) String itemType,
            @McpToolParam(description = "外观/功能描述", required = false) String description,
            @McpToolParam(description = "来源", required = false) String origin,
            @McpToolParam(description = "剧情意义/用途", required = false) String significance,
            @McpToolParam(description = "自定义属性 JSON", required = false) Map<String, Object> properties,
            @McpToolParam(description = "当前持有者", required = false) String currentHolder,
            @McpToolParam(description = "当前位置", required = false) String currentLocation,
            @McpToolParam(description = "当前状态", required = false) String currentStatus,
            @McpToolParam(description = "首次出现章节", required = false) Integer firstChapter,
            @McpToolParam(description = "身份标识 JSON", required = false) Map<String, Object> identity) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        String idJson = identityToString(identity);

        if (!idJson.equals("{}")) {
            if (items.findByProjectAndNameAndIdentity(proj, name, idJson).isPresent())
                throw new IllegalArgumentException("Item already exists: " + name + " with identity " + idJson);
        } else {
            List<Item> existing = items.findByProjectAndName(proj, name);
            if (!existing.isEmpty()) {
                if (existing.size() == 1 && "{}".equals(existing.get(0).getIdentity() != null ? existing.get(0).getIdentity() : "{}"))
                    throw new IllegalArgumentException("Item already exists: " + name);
                throw new IllegalArgumentException("Multiple items named '" + name + "' exist. Specify identity.");
            }
        }

        Item item = new Item();
        item.setProject(proj);
        item.setName(name);
        item.setIdentity(idJson);
        item.setItemType(itemType != null ? itemType : "");
        item.setDescription(description != null ? description : "");
        item.setOrigin(origin != null ? origin : "");
        item.setSignificance(significance != null ? significance : "");
        try {
            item.setProperties(properties != null ? mapper.writeValueAsString(properties) : "{}");
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

        try (RemoteDatabase db = arcadeDB.open(projectId)) {
            db.command("cypher", """
                            MERGE (it:Item {name: $name, identity: $identity})
                            SET it.itemType = $type, it.significance = $sig
                            """,
                    Map.of("name", name, "identity", idJson,
                            "type", item.getItemType(), "sig", item.getSignificance()));

            if (currentHolder != null && !currentHolder.isBlank()) {
                db.command("cypher", """
                                MATCH (it:Item {name: $name, identity: $identity})
                                MERGE (c:Character {name: $holder})
                                MERGE (c)-[:OWNS]->(it)
                                """,
                        Map.of("name", name, "identity", idJson, "holder", currentHolder));
            }
            if (currentLocation != null && !currentLocation.isBlank()) {
                db.command("cypher", """
                                MATCH (it:Item {name: $name, identity: $identity})
                                MERGE (loc:Location {name: $locName})
                                MERGE (loc)-[:CONTAINS]->(it)
                                """,
                        Map.of("name", name, "identity", idJson, "locName", currentLocation));
            }
        } catch (Exception e) {
            log.warn("ArcadeDB item node creation failed for {}/{}", projectId, name, e);
        }

        return new ItemRegisterResult("ok", item.getId().toString(), name);
    }

    @McpTool(name = "item_update", description = "更新物品 | CN 更新物品 / JP アイテムを更新 / EN Update item")
    @Transactional
    public ItemUpdateResult update(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "物品名", required = true) String name,
            @McpToolParam(description = "变更发生的章节号", required = true) Integer chapter,
            @McpToolParam(description = "触发事件", required = true) String event,
            @McpToolParam(description = "新持有者", required = false) String newHolder,
            @McpToolParam(description = "新位置", required = false) String newLocation,
            @McpToolParam(description = "新状态", required = false) String newStatus,
            @McpToolParam(description = "变更描述", required = false) String changeDescription,
            @McpToolParam(description = "身份标识 JSON", required = false) Map<String, Object> identity) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        Item item = resolveItem(proj, name, identity);
        if (item == null) throw new IllegalArgumentException("Item not found: " + name);

        String oldHolder = item.getCurrentHolder();
        if (newHolder != null && !newHolder.isBlank()) item.setCurrentHolder(newHolder);
        if (newLocation != null && !newLocation.isBlank()) item.setCurrentLocation(newLocation);
        if (newStatus != null && !newStatus.isBlank()) item.setCurrentStatus(newStatus);

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("chapter", chapter);
        entry.put("event", event);
        entry.put("from", oldHolder != null ? oldHolder : "");
        entry.put("to", newHolder != null ? newHolder : item.getCurrentHolder());
        entry.put("change", changeDescription != null ? changeDescription : "");
        try {
            List<Map<String, Object>> history;
            String existing = item.getOwnerHistory();
            if (existing != null && !existing.isBlank() && !"[]".equals(existing.trim()))
                history = mapper.readValue(existing, new TypeReference<List<Map<String, Object>>>() {
                });
            else history = new ArrayList<>();
            history.add(entry);
            item.setOwnerHistory(mapper.writeValueAsString(history));
        } catch (Exception e) {
            throw new RuntimeException("Failed to update owner history", e);
        }
        item.setUpdatedAt(Instant.now());
        items.save(item);

        String idJson = item.getIdentity() != null ? item.getIdentity() : "{}";
        try (RemoteDatabase db = arcadeDB.open(projectId)) {
            if (newHolder != null && !newHolder.isBlank() && !newHolder.equals(oldHolder)) {
                if (oldHolder != null && !oldHolder.isBlank()) {
                    db.command("cypher", """
                            MATCH (:Character {name: $old})-[r:OWNS]->(:Item {name: $name, identity: $identity})
                            DELETE r
                            """, Map.of("old", oldHolder, "name", name, "identity", idJson));
                }
                db.command("cypher", """
                        MATCH (it:Item {name: $name, identity: $identity})
                        MERGE (c:Character {name: $holder})
                        MERGE (c)-[:OWNS]->(it)
                        """, Map.of("name", name, "identity", idJson, "holder", newHolder));
            }
        } catch (Exception e) {
            log.warn("ArcadeDB item update failed for {}/{}", projectId, name, e);
        }

        return new ItemUpdateResult("ok", name, chapter, event, newHolder, newStatus);
    }

    @McpTool(name = "item_query", description = "查询物品详情 | CN 查询物品 / JP アイテムを照会 / EN Query item")
    public ItemQueryResult query(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "物品名", required = true) String name,
            @McpToolParam(description = "身份标识 JSON", required = false) Map<String, Object> identity) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        String idJson = identityToString(identity);
        List<Item> matches = !idJson.equals("{}")
                ? Optional.ofNullable(items.findByProjectAndNameAndIdentity(proj, name, idJson).orElse(null)).map(List::of).orElse(List.of())
                : items.findByProjectAndName(proj, name);

        if (matches.isEmpty())
            return new ItemQueryResult("not_found", name, null, Map.of(), List.of(), List.of(), List.of());

        Item item = matches.get(0);
        Map<String, Object> profile = buildItemProfile(item);
        List<Map<String, Object>> history = parseHistory(item.getOwnerHistory(), name);

        List<Map<String, Object>> relations = new ArrayList<>();
        try (RemoteDatabase db = arcadeDB.open(projectId)) {
            var rows = db.query("cypher", """
                    MATCH (it:Item {name: $name, identity: $identity})-[r]-(n)
                    RETURN type(r) AS relType, labels(n) AS nodeLabels, n.name AS nodeName
                    LIMIT 20
                    """, Map.of("name", name, "identity", item.getIdentity() != null ? item.getIdentity() : "{}"));
            while (rows.hasNext()) {
                var row = rows.next();
                Map<String, Object> rel = new LinkedHashMap<>();
                rel.put("type", row.getProperty("relType"));
                rel.put("node", row.getProperty("nodeName"));
                rel.put("nodeType", row.getProperty("nodeLabels"));
                relations.add(rel);
            }
        } catch (Exception e) {
            log.warn("ArcadeDB item relations query failed for {}/{}", projectId, name, e);
        }

        List<Map<String, Object>> allProfiles = matches.size() > 1 ? matches.stream().map(this::buildItemProfile).toList() : List.of();
        String st = matches.size() > 1 && identity == null ? "multiple" : "ok";
        return new ItemQueryResult(st, name, item.getCurrentStatus(), profile, history, relations, allProfiles);
    }

    private Map<String, Object> buildItemProfile(Item item) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("name", item.getName());
        p.put("type", item.getItemType());
        p.put("description", item.getDescription());
        p.put("origin", item.getOrigin());
        p.put("significance", item.getSignificance());
        p.put("identity", item.getIdentity() != null ? item.getIdentity() : "{}");
        try {
            p.put("properties", item.getProperties() != null ? mapper.readValue(item.getProperties(), Object.class) : Map.of());
        } catch (Exception e) {
            p.put("properties", item.getProperties());
        }
        p.put("current_holder", item.getCurrentHolder());
        p.put("current_location", item.getCurrentLocation());
        p.put("current_status", item.getCurrentStatus());
        p.put("first_chapter", item.getFirstChapter());
        return p;
    }

    private List<Map<String, Object>> parseHistory(String json, String name) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) return List.of();
        try {
            return mapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            log.warn("Failed to parse owner_history for item {}", name, e);
            return List.of();
        }
    }

    public record ItemRegisterResult(String status, String itemId, String name) {
    }

    public record ItemUpdateResult(String status, String name, int chapter, String event, String newHolder,
                                   String newStatus) {
    }

    public record ItemQueryResult(String status, String name, String currentStatus, Map<String, Object> profile,
                                  List<Map<String, Object>> ownerHistory, List<Map<String, Object>> relations,
                                  List<Map<String, Object>> allProfiles) {
    }
}
