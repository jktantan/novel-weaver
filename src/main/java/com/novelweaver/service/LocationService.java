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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Component
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final LocationRepository locations;
    private final ProjectRepository projects;

    public LocationService(LocationRepository locations, ProjectRepository projects) {
        this.locations = locations;
        this.projects = projects;
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
            @McpToolParam(description = "叙事功能", required = false) String narrativeFunction) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (locations.findByProjectAndName(proj, name).isPresent()) {
            throw new IllegalArgumentException("Location already exists: " + name);
        }

        Location loc = new Location();
        loc.setProject(proj);
        loc.setName(name);
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
            @McpToolParam(description = "叙事影响", required = false) String narrativeImpact) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        Location loc = locations.findByProjectAndName(proj, name)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + name));

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
                logList = MAPPER.readValue(existing, new TypeReference<List<Map<String, Object>>>() {
                });
            }
            logList.add(entry);
            loc.setChangeLog(MAPPER.writeValueAsString(logList));
        } catch (Exception e) {
            throw new RuntimeException("Failed to update change log", e);
        }

        loc.setCurrentStatus(newStatus);
        loc.setUpdatedAt(Instant.now());
        locations.save(loc);

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
            @McpToolParam(description = "地点名", required = true) String name) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        Location loc = locations.findByProjectAndName(proj, name)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + name));

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("name", loc.getName());
        profile.put("type", loc.getLocationType());
        profile.put("region", loc.getRegion());
        profile.put("first_chapter", loc.getFirstChapter());
        profile.put("canon_description", loc.getCanonDescription());
        profile.put("actual_appearance", loc.getActualAppearance());
        profile.put("sensory_detail", loc.getSensoryDetail());
        profile.put("narrative_function", loc.getNarrativeFunction());

        List<Map<String, Object>> history = new ArrayList<>();
        String changeLogJson = loc.getChangeLog();
        if (changeLogJson != null && !changeLogJson.isBlank() && !"[]".equals(changeLogJson.trim())) {
            try {
                history = MAPPER.readValue(changeLogJson, new TypeReference<List<Map<String, Object>>>() {
                });
            } catch (Exception e) {
                log.warn("Failed to parse change_log for location {}", name, e);
            }
        }

        return new LocationStatusResult("ok", name, loc.getCurrentStatus(), profile, history);
    }

    public record LocationRegisterResult(String status, String locationId, String name) {
    }

    public record LocationUpdateResult(String status, String name, int chapter, String change, String newStatus) {
    }

    public record LocationStatusResult(String status, String name, String currentStatus, Map<String, Object> profile,
                                       List<Map<String, Object>> history) {
    }
}
