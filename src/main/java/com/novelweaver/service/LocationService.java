package com.novelweaver.service;

/*
 * Location Service / 地点管理 / 場所管理
 *
 * CN 地点注册、变更、状态查询 — 图部分使用 ArcadeDB（物理租户）
 * JP 場所登録、変更、状態照会 — グラフは ArcadeDB
 * EN Location — graph via ArcadeDB (physical tenant)
 */

import com.arcadedb.remote.RemoteDatabase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelweaver.config.ArcadeDBManager;
import com.novelweaver.model.Location;
import com.novelweaver.model.Project;
import com.novelweaver.repository.LocationRepository;
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
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private final LocationRepository locations;
    private final ProjectRepository projects;
    private final ArcadeDBManager arcadeDB;
    private final ObjectMapper mapper;

    public LocationService(LocationRepository locations, ProjectRepository projects,
                           ArcadeDBManager arcadeDB, ObjectMapper mapper) {
        this.locations = locations;
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

    private Location resolveLocation(Project proj, String name, Map<String, Object> identity) {
        String idJson = identityToString(identity);
        if (!idJson.equals("{}")) return locations.findByProjectAndNameAndIdentity(proj, name, idJson).orElse(null);
        List<Location> matches = locations.findByProjectAndName(proj, name);
        if (matches.isEmpty()) return null;
        if (matches.size() == 1) return matches.get(0);
        List<String> ids = matches.stream()
                .map(l -> "  - " + l.getName() + " [" + (l.getIdentity() != null ? l.getIdentity() : "{}") + "]")
                .toList();
        throw new IllegalArgumentException("Multiple locations named '" + name + "' found. Specify identity:\n" + String.join("\n", ids));
    }

    @McpTool(name = "location_register", description = "注册新地点 | CN 注册新地点 / JP 新しい場所を登録 / EN Register new location")
    @Transactional
    public LocationRegisterResult register(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "地点名", required = true) String name,
            @McpToolParam(description = "类型", required = false) String locationType,
            @McpToolParam(description = "所属区域", required = false) String region,
            @McpToolParam(description = "首次出现章节", required = false) Integer firstChapter,
            @McpToolParam(description = "正典/原始设定", required = false) String canonDescription,
            @McpToolParam(description = "实际外观", required = false) String actualAppearance,
            @McpToolParam(description = "感官细节", required = false) String sensoryDetail,
            @McpToolParam(description = "叙事功能", required = false) String narrativeFunction,
            @McpToolParam(description = "身份标识 JSON", required = false) Map<String, Object> identity) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        String idJson = identityToString(identity);

        if (!idJson.equals("{}")) {
            if (locations.findByProjectAndNameAndIdentity(proj, name, idJson).isPresent())
                throw new IllegalArgumentException("Location already exists: " + name + " with identity " + idJson);
        } else {
            List<Location> existing = locations.findByProjectAndName(proj, name);
            if (!existing.isEmpty()) {
                if (existing.size() == 1 && "{}".equals(existing.get(0).getIdentity() != null ? existing.get(0).getIdentity() : "{}"))
                    throw new IllegalArgumentException("Location already exists: " + name);
                List<String> ids = existing.stream().map(l -> "  - identity: " + (l.getIdentity() != null ? l.getIdentity() : "{}")).toList();
                throw new IllegalArgumentException("Multiple locations named '" + name + "' exist. Specify identity:\n" + String.join("\n", ids));
            }
        }

        Location loc = new Location();
        loc.setProject(proj);
        loc.setName(name);
        loc.setIdentity(idJson);
        loc.setLocationType(locationType);
        loc.setRegion(region);
        loc.setFirstChapter(firstChapter != null ? firstChapter : 1);
        loc.setCanonDescription(canonDescription);
        loc.setActualAppearance(actualAppearance);
        loc.setSensoryDetail(sensoryDetail);
        loc.setNarrativeFunction(narrativeFunction);
        loc.setChangeLog("[]");
        loc.setCurrentStatus("正常");
        loc.setCreatedAt(Instant.now());
        loc.setUpdatedAt(Instant.now());
        locations.save(loc);

        try (RemoteDatabase db = arcadeDB.open(projectId)) {
            db.command("cypher", """
                            MERGE (loc:Location {name: $name, identity: $identity})
                            SET loc.locationType = $type, loc.region = $region
                            """,
                    Map.of("name", name, "identity", idJson,
                            "type", locationType != null ? locationType : "",
                            "region", region != null ? region : ""));
        } catch (Exception e) {
            log.warn("ArcadeDB location node creation failed for {}/{}", projectId, name, e);
        }

        return new LocationRegisterResult("ok", loc.getId().toString(), name);
    }

    @McpTool(name = "location_update", description = "记录地点变更 | CN 记录地点变更 / JP 場所の変更を記録 / EN Record location change")
    @Transactional
    public LocationUpdateResult update(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "地点名", required = true) String name,
            @McpToolParam(description = "触发变更的章节号", required = true) Integer chapter,
            @McpToolParam(description = "触发事件", required = true) String triggerEvent,
            @McpToolParam(description = "变更内容", required = true) String change,
            @McpToolParam(description = "变更后状态", required = true) String newStatus,
            @McpToolParam(description = "叙事影响", required = false) String narrativeImpact,
            @McpToolParam(description = "身份标识 JSON", required = false) Map<String, Object> identity) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        Location loc = resolveLocation(proj, name, identity);
        if (loc == null) throw new IllegalArgumentException("Location not found: " + name);

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("chapter", chapter);
        entry.put("trigger_event", triggerEvent);
        entry.put("change", change);
        entry.put("new_status", newStatus);
        entry.put("narrative_impact", narrativeImpact != null ? narrativeImpact : "");

        try {
            List<Map<String, Object>> logList = new ArrayList<>();
            String existing = loc.getChangeLog();
            if (existing != null && !existing.isBlank() && !"[]".equals(existing.trim()))
                logList = mapper.readValue(existing, new TypeReference<List<Map<String, Object>>>() {
                });
            logList.add(entry);
            loc.setChangeLog(mapper.writeValueAsString(logList));
        } catch (Exception e) {
            throw new RuntimeException("Failed to update change log", e);
        }

        loc.setCurrentStatus(newStatus);
        loc.setUpdatedAt(Instant.now());
        locations.save(loc);

        String idJson = loc.getIdentity() != null ? loc.getIdentity() : "{}";
        try (RemoteDatabase db = arcadeDB.open(projectId)) {
            db.command("cypher", """
                            MATCH (loc:Location {name: $locName, identity: $identity})
                            MATCH (ch:Chapter {number: $chNum})
                            MERGE (loc)-[:APPEARS_IN]->(ch)
                            """,
                    Map.of("locName", name, "identity", idJson, "chNum", chapter));
        } catch (Exception e) {
            log.warn("ArcadeDB location APPEARS_IN failed for {}/{} chapter {}", projectId, name, chapter, e);
        }

        return new LocationUpdateResult("ok", name, chapter, change, newStatus);
    }

    @McpTool(name = "location_status", description = "查询地点当前状态 | CN 查询地点状态 / JP 場所の状態照会 / EN Query location status")
    public LocationStatusResult status(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "地点名", required = true) String name,
            @McpToolParam(description = "身份标识 JSON", required = false) Map<String, Object> identity) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        String idJson = identityToString(identity);
        List<Location> matches = !idJson.equals("{}")
                ? Optional.ofNullable(locations.findByProjectAndNameAndIdentity(proj, name, idJson).orElse(null)).map(List::of).orElse(List.of())
                : locations.findByProjectAndName(proj, name);

        if (matches.isEmpty()) return new LocationStatusResult("not_found", name, null, Map.of(), List.of(), List.of());

        Location loc = matches.get(0);
        Map<String, Object> profile = buildProfile(loc);
        List<Map<String, Object>> history = parseHistory(loc.getChangeLog(), name);
        List<Map<String, Object>> allProfiles = matches.size() > 1 ? matches.stream().map(this::buildProfile).toList() : List.of();
        String st = matches.size() > 1 && identity == null ? "multiple" : "ok";
        return new LocationStatusResult(st, name, loc.getCurrentStatus(), profile, history, allProfiles);
    }

    private Map<String, Object> buildProfile(Location loc) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("name", loc.getName());
        p.put("type", loc.getLocationType());
        p.put("region", loc.getRegion());
        p.put("first_chapter", loc.getFirstChapter());
        p.put("canon_description", loc.getCanonDescription());
        p.put("actual_appearance", loc.getActualAppearance());
        p.put("sensory_detail", loc.getSensoryDetail());
        p.put("narrative_function", loc.getNarrativeFunction());
        p.put("identity", loc.getIdentity() != null ? loc.getIdentity() : "{}");
        return p;
    }

    private List<Map<String, Object>> parseHistory(String json, String name) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) return List.of();
        try {
            return mapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            log.warn("Failed to parse change_log for location {}", name, e);
            return List.of();
        }
    }

    public record LocationRegisterResult(String status, String locationId, String name) {
    }

    public record LocationUpdateResult(String status, String name, int chapter, String change, String newStatus) {
    }
    public record LocationStatusResult(String status, String name, String currentStatus, Map<String, Object> profile,
                                       List<Map<String, Object>> history, List<Map<String, Object>> allProfiles) {
    }
}
