package com.novelweaver.service;

/*
 * Location Service / 地点管理 / 場所管理
 *
 * CN 地点注册、变更、状态查询
 * JP 場所登録、変更、状態照会
 * EN Location register, update, status query
 */

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelweaver.model.Location;
import com.novelweaver.model.Project;
import com.novelweaver.repository.LocationRepository;
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
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private final LocationRepository locations;
    private final ProjectRepository projects;
    private final Neo4jClient neo4j;
    private final ObjectMapper mapper;

    public LocationService(LocationRepository locations, ProjectRepository projects,
                           Neo4jClient neo4j, ObjectMapper mapper) {
        this.locations = locations;
        this.projects = projects;
        this.neo4j = neo4j;
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
        if (!idJson.equals("{}")) {
            return locations.findByProjectAndNameAndIdentity(proj, name, idJson).orElse(null);
        }
        List<Location> matches = locations.findByProjectAndName(proj, name);
        if (matches.isEmpty()) return null;
        if (matches.size() == 1) return matches.get(0);
        List<String> ids = matches.stream()
                .map(l -> "  - " + l.getName() + " [" + (l.getIdentity() != null ? l.getIdentity() : "{}") + "]")
                .toList();
        throw new IllegalArgumentException(
                "Multiple locations named '" + name + "' found. Please specify identity to disambiguate:\n"
                        + String.join("\n", ids));
    }


    /*
     * 注册地点 / 登録 / Register
     *
     * CN 注册新地点，记录初始外观和感官细节
     * JP 新しい場所を登録、初期外観と感覚詳細を記録
     * EN Register new location with initial appearance and sensory details
     */
    @McpTool(name = "location_register", description = "注册新地点——记录初始外观、感官细节、叙事用途 | CN 注册新地点 / JP 新しい場所を登録 / EN Register new location")
    @Transactional
    public LocationRegisterResult register(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "地点名", required = true) String name,
            @McpToolParam(description = "类型（自然场景/建筑/聚落/室内）", required = false) String locationType,
            @McpToolParam(description = "所属区域", required = false) String region,
            @McpToolParam(description = "首次出现章节", required = false) Integer firstChapter,
            @McpToolParam(description = "正典/原始设定（同人可查 wiki）", required = false) String canonDescription,
            @McpToolParam(description = "实际外观（叙事视角看到的）", required = false) String actualAppearance,
            @McpToolParam(description = "感官细节（视觉/听觉/嗅觉/触觉）", required = false) String sensoryDetail,
            @McpToolParam(description = "叙事功能", required = false) String narrativeFunction,
            @McpToolParam(description = "身份标识 JSON（区分同名地点，如 {\"era\":\"黄金时代\"}）", required = false) Map<String, Object> identity) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        String idJson = identityToString(identity);

        if (!idJson.equals("{}")) {
            if (locations.findByProjectAndNameAndIdentity(proj, name, idJson).isPresent()) {
                throw new IllegalArgumentException("Location already exists: " + name + " with identity " + idJson);
            }
        } else {
            List<Location> existing = locations.findByProjectAndName(proj, name);
            if (!existing.isEmpty()) {
                if (existing.size() == 1 && "{}".equals(existing.get(0).getIdentity() != null ? existing.get(0).getIdentity() : "{}")) {
                    throw new IllegalArgumentException("Location already exists: " + name);
                }
                List<String> ids = existing.stream()
                        .map(l -> "  - identity: " + (l.getIdentity() != null ? l.getIdentity() : "{}"))
                        .toList();
                throw new IllegalArgumentException(
                        "Multiple locations named '" + name + "' exist. Please specify identity:\n"
                                + String.join("\n", ids));
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

        // Neo4j: create :Location node (with identity in key)
        try {
            neo4j.query("""
                            MERGE (loc:Location {project_id: $pid, name: $name, identity: $identity})
                            SET loc.locationType = $type, loc.region = $region
                            """)
                    .bind(projectId).to("pid")
                    .bind(name).to("name")
                    .bind(idJson).to("identity")
                    .bind(locationType != null ? locationType : "").to("type")
                    .bind(region != null ? region : "").to("region")
                    .run();
        } catch (Exception e) {
            log.warn("Neo4j location node creation failed for {}/{}", projectId, name, e);
        }

        return new LocationRegisterResult("ok", loc.getId().toString(), name);
    }


    /*
     * 更新地点 / 更新 / Update
     *
     * CN 追加地点变更记录，自动更新当前状态
     * JP 場所の変更記録を追加、現在状態を自動更新
     * EN Append location change, auto-update current status
     */
    @McpTool(name = "location_update", description = "记录地点变更——追加变更记录，自动更新当前状态 | CN 记录地点变更 / JP 場所の変更を記録 / EN Record location change")
    @Transactional
    public LocationUpdateResult update(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "地点名", required = true) String name,
            @McpToolParam(description = "触发变更的章节号", required = true) Integer chapter,
            @McpToolParam(description = "触发事件", required = true) String triggerEvent,
            @McpToolParam(description = "变更内容", required = true) String change,
            @McpToolParam(description = "变更后状态", required = true) String newStatus,
            @McpToolParam(description = "叙事影响", required = false) String narrativeImpact,
            @McpToolParam(description = "身份标识 JSON（区分同名地点）", required = false) Map<String, Object> identity) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        Location loc = resolveLocation(proj, name, identity);
        if (loc == null) {
            String hint = identity != null ? " with identity " + identityToString(identity) : "";
            throw new IllegalArgumentException("Location not found: " + name + hint);
        }

        // 构建新变更记录
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("chapter", chapter);
        entry.put("trigger_event", triggerEvent);
        entry.put("change", change);
        entry.put("new_status", newStatus);
        entry.put("narrative_impact", narrativeImpact != null ? narrativeImpact : "");

        // 追加到现有 change_log
        try {
            List<Map<String, Object>> logList = new ArrayList<>();
            String existing = loc.getChangeLog();
            if (existing != null && !existing.isBlank() && !"[]".equals(existing.trim())) {
                logList = mapper.readValue(existing, new TypeReference<List<Map<String, Object>>>() {
                });
            }
            logList.add(entry);
            loc.setChangeLog(mapper.writeValueAsString(logList));
        } catch (Exception e) {
            throw new RuntimeException("Failed to update change log", e);
        }

        loc.setCurrentStatus(newStatus);
        loc.setUpdatedAt(Instant.now());
        locations.save(loc);

        // Neo4j: link location to the triggering chapter
        String idJson = loc.getIdentity() != null ? loc.getIdentity() : "{}";
        try {
            neo4j.query("""
                            MATCH (loc:Location {project_id: $pid, name: $locName, identity: $identity})
                            MATCH (ch:Chapter {project_id: $pid, number: $chNum})
                            MERGE (loc)-[:APPEARS_IN]->(ch)
                            """)
                    .bind(projectId).to("pid")
                    .bind(name).to("locName")
                    .bind(idJson).to("identity")
                    .bind(chapter).to("chNum")
                    .run();
        } catch (Exception e) {
            log.warn("Neo4j location APPEARS_IN failed for {}/{} chapter {}", projectId, name, chapter, e);
        }

        return new LocationUpdateResult("ok", name, chapter, change, newStatus);
    }


    /*
     * 查询地点 / 照会 / Status
     *
     * CN 查询地点初始信息 + 全部变更记录 + 当前状态
     * JP 場所の初期情報+全変更記録+現在状態を照会
     * EN Query location init info + all changes + current status
     */
    @McpTool(name = "location_status", description = "查询地点当前状态——返回初始信息 + 全部变更记录 + 当前状态 | CN 查询地点状态 / JP 場所の状態照会 / EN Query location status")
    public LocationStatusResult status(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "地点名", required = true) String name,
            @McpToolParam(description = "身份标识 JSON（区分同名地点，不传则返回全部匹配）", required = false) Map<String, Object> identity) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        String idJson = identityToString(identity);
        List<Location> matches;

        if (!idJson.equals("{}")) {
            Location loc = locations.findByProjectAndNameAndIdentity(proj, name, idJson).orElse(null);
            matches = loc != null ? List.of(loc) : List.of();
        } else {
            matches = locations.findByProjectAndName(proj, name);
        }

        if (matches.isEmpty()) {
            return new LocationStatusResult("not_found", name, null, Map.of(), List.of(), List.of());
        }

        Location loc = matches.get(0);
        Map<String, Object> profile = buildLocationProfile(loc);

        List<Map<String, Object>> history = new ArrayList<>();
        String changeLogJson = loc.getChangeLog();
        if (changeLogJson != null && !changeLogJson.isBlank() && !"[]".equals(changeLogJson.trim())) {
            try {
                history = mapper.readValue(changeLogJson, new TypeReference<List<Map<String, Object>>>() {
                });
            } catch (Exception e) {
                log.warn("Failed to parse change_log for location {}", name, e);
            }
        }

        List<Map<String, Object>> allProfiles = matches.size() > 1
                ? matches.stream().map(this::buildLocationProfile).toList()
                : List.of();

        String status = matches.size() > 1 && identity == null ? "multiple" : "ok";
        return new LocationStatusResult(status, name, loc.getCurrentStatus(), profile, history, allProfiles);
    }

    private Map<String, Object> buildLocationProfile(Location loc) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("name", loc.getName());
        profile.put("type", loc.getLocationType());
        profile.put("region", loc.getRegion());
        profile.put("first_chapter", loc.getFirstChapter());
        profile.put("canon_description", loc.getCanonDescription());
        profile.put("actual_appearance", loc.getActualAppearance());
        profile.put("sensory_detail", loc.getSensoryDetail());
        profile.put("narrative_function", loc.getNarrativeFunction());
        profile.put("identity", loc.getIdentity() != null ? loc.getIdentity() : "{}");
        return profile;
    }

    public record LocationRegisterResult(String status, String locationId, String name) {
    }

    public record LocationUpdateResult(String status, String name, int chapter, String change, String newStatus) {
    }

    public record LocationStatusResult(String status, String name, String currentStatus, Map<String, Object> profile,
                                       List<Map<String, Object>> history,
                                       List<Map<String, Object>> allProfiles) {
    }
}
