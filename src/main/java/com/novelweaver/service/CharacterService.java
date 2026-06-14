package com.novelweaver.service;

/*
 * Character Service / 人物管理 / キャラクター管理
 *
 * CN 人物画像、状态快照、冲突检测
 * JP キャラクタープロフィール、状態スナップショット、競合検出
 * EN Character profile, snapshot, conflict detection
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelweaver.model.Chapter;
import com.novelweaver.model.CharacterProfile;
import com.novelweaver.model.CharacterSnapshot;
import com.novelweaver.model.Project;
import com.novelweaver.repository.ChapterRepository;
import com.novelweaver.repository.CharacterProfileRepository;
import com.novelweaver.repository.CharacterSnapshotRepository;
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
public class CharacterService {

    private static final Logger log = LoggerFactory.getLogger(CharacterService.class);

    private final CharacterProfileRepository profiles;
    private final CharacterSnapshotRepository snapshots;
    private final ChapterRepository chapters;
    private final ProjectRepository projects;
    private final ObjectMapper mapper;
    private final Neo4jClient neo4j;

    public CharacterService(CharacterProfileRepository profiles, CharacterSnapshotRepository snapshots,
                            ChapterRepository chapters, ProjectRepository projects,
                            ObjectMapper mapper, Neo4jClient neo4j) {
        this.profiles = profiles;
        this.snapshots = snapshots;
        this.chapters = chapters;
        this.projects = projects;
        this.mapper = mapper;
        this.neo4j = neo4j;
    }

    /*
     * 保存画像 / 保存 / Save
     *
     * CN 创建/更新人物画像，含声线约束
     * JP キャラクタープロフィールを作成/更新、声色制約を含む
     * EN Create/update character profile with voice constraints
     */
    @McpTool(name = "character_save", description = "创建/更新人物画像 | CN 创建/更新人物画像 / JP キャラクタープロフィール作成/更新 / EN Create/update character")
    @Transactional
    public CharacterSaveResult save(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "角色名", required = true) String name,
            @McpToolParam(description = "简介", required = false) String bio,
            @McpToolParam(description = "性格特征", required = false) List<String> traits,
            @McpToolParam(description = "声线种子台词", required = false) List<String> voiceSeeds,
            @McpToolParam(description = "声线硬约束 JSON", required = false) Map<String, Object> voiceMeta) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        CharacterProfile cp = profiles.findByProjectAndName(proj, name)
                .orElseGet(() -> {
                    CharacterProfile c = new CharacterProfile();
                    c.setProject(proj);
                    c.setName(name);
                    c.setCreatedAt(Instant.now());
                    return c;
                });

        if (bio != null) cp.setBio(bio);
        if (traits != null) {
            try {
                cp.setTraits(mapper.writeValueAsString(traits));
            } catch (Exception e) {
                cp.setTraits("[]");
            }
        }
        if (voiceSeeds != null) cp.setVoiceSeeds(voiceSeeds.toArray(new String[0]));
        if (voiceMeta != null) {
            try {
                cp.setVoiceMeta(mapper.writeValueAsString(voiceMeta));
            } catch (Exception e) {
                cp.setVoiceMeta("{}");
            }
        }
        cp.setUpdatedAt(Instant.now());
        profiles.save(cp);

        return new CharacterSaveResult("ok", cp.getId().toString(), name, "saved");
    }

    /*
     * 查询状态 / 照会 / Status
     *
     * CN 查询角色当前状态 + 历史快照
     * JP キャラクターの現在状態 + 履歴スナップショット
     * EN Query character current status + history
     */
    @McpTool(name = "character_status", description = "获取角色当前状态 + 历史快照 | CN 查询角色状态 / JP キャラクター状態照会 / EN Query character status")
    public CharacterStatusResult status(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "角色名", required = true) String name) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        CharacterProfile cp = profiles.findByProjectAndName(proj, name).orElse(null);
        if (cp == null) {
            return new CharacterStatusResult("not_found", name, null, List.of());
        }

        // 目标查询——只查该角色
        List<CharacterSnapshot> snaps = snapshots.findByProjectAndCharacterNameIn(proj, List.of(name));
        List<SnapshotInfo> history = snaps.stream()
                .map(s -> new SnapshotInfo(
                        s.getChapter().getChapterNumber(),
                        s.getPhysicalLocation(),
                        s.getPhysicalStatus(),
                        s.getCorePsychology(),
                        s.getKeyItems() != null ? Arrays.asList(s.getKeyItems()) : List.of(),
                        s.getSummary()))
                .toList();

        ProfileInfo profile = new ProfileInfo(
                cp.getId().toString(), name, cp.getBio(), cp.getVoice(),
                cp.getVoiceSeeds() != null ? Arrays.asList(cp.getVoiceSeeds()) : List.of(),
                cp.getType());

        return new CharacterStatusResult("ok", name, profile, history);
    }

    // ── serialization ──

    /*
     * 记录快照 / 記録 / Snapshot
     *
     * CN 记录一章后角色状态快照
     * JP 章終了後のキャラクター状態を記録
     * EN Record character state after a chapter
     */
    @McpTool(name = "character_snapshot", description = "记录一章后的人物状态 | CN 记录角色快照 / JP キャラクター状態記録 / EN Record character snapshot")
    @Transactional
    public CharacterSnapshotResult snapshot(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "角色名", required = true) String name,
            @McpToolParam(description = "章节号", required = true) int chapterNumber,
            @McpToolParam(description = "位置", required = false) String location,
            @McpToolParam(description = "生理状态", required = false) String physical,
            @McpToolParam(description = "心理状态", required = false) String psychology,
            @McpToolParam(description = "关键物品", required = false) List<String> items,
            @McpToolParam(description = "状态摘要", required = false) String summary) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        CharacterProfile cp = profiles.findByProjectAndName(proj, name)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + name));
        Chapter chapter = chapters.findByProjectAndChapterNumber(proj, chapterNumber)
                .orElseThrow(() -> new IllegalArgumentException("Chapter not found: " + chapterNumber));

        CharacterSnapshot cs = snapshots.findByChapterAndCharacterName(chapter, name)
                .orElseGet(() -> {
                    CharacterSnapshot s = new CharacterSnapshot();
                    s.setCharacter(cp);
                    s.setChapter(chapter);
                    s.setProject(proj);
                    s.setCharacterName(name);
                    s.setCreatedAt(Instant.now());
                    return s;
                });

        if (location != null) cs.setPhysicalLocation(location);
        if (physical != null) cs.setPhysicalStatus(physical);
        if (psychology != null) cs.setCorePsychology(psychology);
        if (items != null) cs.setKeyItems(items.toArray(new String[0]));
        if (summary != null) cs.setSummary(summary);
        cs.setUpdatedAt(Instant.now());
        snapshots.save(cs);

        // Neo4j: record character visited location
        if (location != null && !location.isBlank()) {
            try {
                neo4j.query("""
                                MERGE (c:Character {project_id: $pid, name: $charName})
                                MERGE (loc:Location {project_id: $pid, name: $locName})
                                MERGE (c)-[:VISITED]->(loc)
                                """)
                        .bind(projectId).to("pid")
                        .bind(name).to("charName")
                        .bind(location).to("locName")
                        .run();
            } catch (Exception e) {
                log.warn("Neo4j VISITED edge failed for {}/{} -> {}", projectId, name, location, e);
            }
        }

        return new CharacterSnapshotResult("ok", name, chapterNumber);
    }

    /*
     * 冲突检测 / 競合検出 / Conflict check
     *
     * CN 检测修改某章会影响哪些后续快照
     * JP 章の修正が後続スナップショットに与える影響を検出
     * EN Detect which later snapshots are affected by a chapter change
     */
    @McpTool(name = "character_snapshot_check", description = "检测修改某章会影响哪些后续角色的快照——给定要修改的章节号，查出该章涉及的角色在哪些后续章节有快照 | CN 检测修改影响 / JP 変更の影響を検出 / EN Detect modification impact")
    public SnapshotCheckResult snapshotCheck(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "要修改的章节号", required = true) int modifiedChapter) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        // 1. 找出该章节涉及的所有角色
        List<String> charNames = snapshots.findCharacterNamesInChapter(proj, modifiedChapter);
        if (charNames.isEmpty()) {
            return new SnapshotCheckResult(modifiedChapter, List.of(),
                    "第" + modifiedChapter + "章没有角色快照记录，无需检查");
        }

        // 2. 找出这些角色在后续章节的快照
        List<CharacterSnapshot> all = snapshots.findByProjectAndCharacterNameIn(proj, charNames);

        // 3. 按角色名分组，只保留 > modifiedChapter 的记录
        Map<String, List<Map<String, Object>>> affected = new LinkedHashMap<>();
        for (String name : charNames) {
            List<Map<String, Object>> laterChapters = all.stream()
                    .filter(s -> s.getCharacterName().equals(name)
                            && s.getChapter().getChapterNumber() > modifiedChapter)
                    .map(s -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("chapter", s.getChapter().getChapterNumber());
                        m.put("location", s.getPhysicalLocation());
                        m.put("psychology", s.getCorePsychology());
                        m.put("summary", s.getSummary());
                        return m;
                    })
                    .sorted(Comparator.comparingInt(m -> (Integer) m.get("chapter")))
                    .toList();
            if (!laterChapters.isEmpty()) {
                affected.put(name, laterChapters);
            }
        }

        if (affected.isEmpty()) {
            return new SnapshotCheckResult(modifiedChapter, List.of(),
                    "第" + modifiedChapter + "章涉及的角色在后续章节中没有快照，无需检查");
        }

        // 4. 组装结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (var entry : affected.entrySet()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("character", entry.getKey());
            r.put("affected_chapters", entry.getValue());
            result.add(r);
        }

        return new SnapshotCheckResult(modifiedChapter, result, null);
    }

    // ── result records ──

    public record CharacterSaveResult(String status, String characterId, String name, String action) {
    }

    public record CharacterStatusResult(String status, String name, ProfileInfo profile,
                                        List<SnapshotInfo> history) {
    }

    public record ProfileInfo(String id, String name, String bio, String voice,
                              List<String> voiceSeeds, String type) {
    }

    public record SnapshotInfo(int chapterNumber, String location, String physical,
                               String psychology, List<String> items, String summary) {
    }

    public record CharacterSnapshotResult(String status, String name, int chapterNumber) {
    }

    public record SnapshotCheckResult(int modifiedChapter, List<Map<String, Object>> affectedCharacters, String note) {
    }
}
