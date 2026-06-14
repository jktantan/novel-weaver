package com.novelweaver.service;

/*
 * Character Service / 人物管理 / キャラクター管理
 *
 * CN 人物画像、状态快照、冲突检测 — 图部分使用 ArcadeDB（物理租户）
 * JP キャラクタープロフィール、状態スナップショット — グラフは ArcadeDB
 * EN Character profile, snapshot — graph via ArcadeDB (physical tenant)
 */

import com.arcadedb.remote.RemoteDatabase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelweaver.config.ArcadeDBManager;
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
    private final ArcadeDBManager arcadeDB;

    public CharacterService(CharacterProfileRepository profiles, CharacterSnapshotRepository snapshots,
                            ChapterRepository chapters, ProjectRepository projects,
                            ObjectMapper mapper, ArcadeDBManager arcadeDB) {
        this.profiles = profiles;
        this.snapshots = snapshots;
        this.chapters = chapters;
        this.projects = projects;
        this.mapper = mapper;
        this.arcadeDB = arcadeDB;
    }

    private String identityToString(Map<String, Object> identity) {
        if (identity == null || identity.isEmpty()) return "{}";
        try {
            return mapper.writeValueAsString(identity);
        } catch (Exception e) {
            return "{}";
        }
    }

    private CharacterProfile newProfile(Project proj, String name, String identity) {
        CharacterProfile c = new CharacterProfile();
        c.setProject(proj);
        c.setName(name);
        c.setIdentity(identity != null ? identity : "{}");
        c.setCreatedAt(Instant.now());
        return c;
    }

    private CharacterProfile resolveProfile(Project proj, String name, Map<String, Object> identity, boolean forWrite) {
        String idJson = identityToString(identity);
        if (!idJson.equals("{}")) {
            return profiles.findByProjectAndNameAndIdentity(proj, name, idJson).orElse(null);
        }
        List<CharacterProfile> matches = profiles.findByProjectAndName(proj, name);
        if (matches.isEmpty()) return null;
        if (matches.size() == 1) return matches.get(0);
        List<String> ids = matches.stream()
                .map(c -> "  - " + c.getName() + " [" + (c.getIdentity() != null ? c.getIdentity() : "{}") + "]")
                .toList();
        throw new IllegalArgumentException(
                "Multiple characters named '" + name + "' found. Please specify identity to disambiguate:\n"
                        + String.join("\n", ids));
    }

    @McpTool(name = "character_save", description = "创建/更新人物画像 | CN 创建/更新人物画像 / JP キャラクタープロフィール作成/更新 / EN Create/update character")
    @Transactional
    public CharacterSaveResult save(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "角色名", required = true) String name,
            @McpToolParam(description = "简介", required = false) String bio,
            @McpToolParam(description = "性格特征", required = false) List<String> traits,
            @McpToolParam(description = "声线种子台词", required = false) List<String> voiceSeeds,
            @McpToolParam(description = "声线硬约束 JSON", required = false) Map<String, Object> voiceMeta,
            @McpToolParam(description = "身份标识 JSON", required = false) Map<String, Object> identity) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        String idJson = identityToString(identity);
        CharacterProfile cp;

        if (!idJson.equals("{}")) {
            cp = profiles.findByProjectAndNameAndIdentity(proj, name, idJson)
                    .orElseGet(() -> newProfile(proj, name, idJson));
        } else {
            List<CharacterProfile> matches = profiles.findByProjectAndName(proj, name);
            if (matches.size() > 1) {
                List<String> ids = matches.stream()
                        .map(c -> "  - identity: " + (c.getIdentity() != null ? c.getIdentity() : "{}"))
                        .toList();
                throw new IllegalArgumentException(
                        "Multiple characters named '" + name + "' exist. Please specify identity:\n"
                                + String.join("\n", ids));
            }
            cp = matches.isEmpty() ? newProfile(proj, name, "{}") : matches.get(0);
        }

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

    @McpTool(name = "character_status", description = "获取角色当前状态 + 历史快照 | CN 查询角色状态 / JP キャラクター状態照会 / EN Query character status")
    public CharacterStatusResult status(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "角色名", required = true) String name,
            @McpToolParam(description = "身份标识 JSON", required = false) Map<String, Object> identity) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        String idJson = identityToString(identity);
        List<CharacterProfile> matches;

        if (!idJson.equals("{}")) {
            CharacterProfile cp = profiles.findByProjectAndNameAndIdentity(proj, name, idJson).orElse(null);
            matches = cp != null ? List.of(cp) : List.of();
        } else {
            matches = profiles.findByProjectAndName(proj, name);
        }

        if (matches.isEmpty()) {
            return new CharacterStatusResult("not_found", name, null, List.of(), List.of());
        }

        List<ProfileInfo> profileList = new ArrayList<>();
        List<SnapshotInfo> history = new ArrayList<>();

        for (CharacterProfile cp : matches) {
            ProfileInfo pi = new ProfileInfo(cp.getId().toString(), name, cp.getBio(), cp.getVoice(),
                    cp.getVoiceSeeds() != null ? Arrays.asList(cp.getVoiceSeeds()) : List.of(),
                    cp.getType(), cp.getIdentity() != null ? cp.getIdentity() : "{}");
            profileList.add(pi);

            List<CharacterSnapshot> snaps = snapshots.findByProjectAndCharacterNameIn(proj, List.of(name));
            for (CharacterSnapshot s : snaps) {
                if (s.getCharacter().getId().equals(cp.getId())) {
                    history.add(new SnapshotInfo(s.getChapter().getChapterNumber(), s.getPhysicalLocation(),
                            s.getPhysicalStatus(), s.getCorePsychology(),
                            s.getKeyItems() != null ? Arrays.asList(s.getKeyItems()) : List.of(),
                            s.getSummary()));
                }
            }
        }

        String st = matches.size() > 1 && identity == null ? "multiple" : "ok";
        return new CharacterStatusResult(st, name, profileList.get(0), history,
                matches.size() > 1 ? profileList : List.of());
    }

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
            @McpToolParam(description = "状态摘要", required = false) String summary,
            @McpToolParam(description = "身份标识 JSON", required = false) Map<String, Object> identity) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        CharacterProfile cp = resolveProfile(proj, name, identity, false);
        if (cp == null) {
            String hint = identity != null ? " with identity " + identityToString(identity) : "";
            throw new IllegalArgumentException("Character not found: " + name + hint);
        }
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

        // ArcadeDB: record character visited location
        if (location != null && !location.isBlank()) {
            try (RemoteDatabase db = arcadeDB.open(projectId)) {
                String idJson = cp.getIdentity() != null ? cp.getIdentity() : "{}";
                db.command("cypher", """
                                MERGE (c:Character {name: $charName, identity: $identity})
                                MERGE (loc:Location {name: $locName})
                                MERGE (c)-[:VISITED]->(loc)
                                """,
                        Map.of("charName", name, "identity", idJson, "locName", location));
            } catch (Exception e) {
                log.warn("ArcadeDB VISITED edge failed for {}/{} -> {}", projectId, name, location, e);
            }
        }

        return new CharacterSnapshotResult("ok", name, chapterNumber);
    }

    @McpTool(name = "character_snapshot_check", description = "检测修改影响 | CN 检测修改影响 / JP 変更の影響を検出 / EN Detect modification impact")
    public SnapshotCheckResult snapshotCheck(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "要修改的章节号", required = true) int modifiedChapter) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        List<String> charNames = snapshots.findCharacterNamesInChapter(proj, modifiedChapter);
        if (charNames.isEmpty()) {
            return new SnapshotCheckResult(modifiedChapter, List.of(),
                    "第" + modifiedChapter + "章没有角色快照记录，无需检查");
        }

        List<CharacterSnapshot> all = snapshots.findByProjectAndCharacterNameIn(proj, charNames);

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
            if (!laterChapters.isEmpty()) affected.put(name, laterChapters);
        }

        if (affected.isEmpty()) {
            return new SnapshotCheckResult(modifiedChapter, List.of(),
                    "第" + modifiedChapter + "章涉及的角色在后续章节中没有快照，无需检查");
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (var entry : affected.entrySet()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("character", entry.getKey());
            r.put("affected_chapters", entry.getValue());
            result.add(r);
        }

        return new SnapshotCheckResult(modifiedChapter, result, null);
    }

    public record CharacterSaveResult(String status, String characterId, String name, String action) {
    }
    public record CharacterStatusResult(String status, String name, ProfileInfo profile,
                                        List<SnapshotInfo> history, List<ProfileInfo> allProfiles) {
    }
    public record ProfileInfo(String id, String name, String bio, String voice,
                              List<String> voiceSeeds, String type, String identity) {
    }
    public record SnapshotInfo(int chapterNumber, String location, String physical,
                               String psychology, List<String> items, String summary) {
    }

    public record CharacterSnapshotResult(String status, String name, int chapterNumber) {
    }

    public record SnapshotCheckResult(int modifiedChapter, List<Map<String, Object>> affectedCharacters, String note) {
    }
}
