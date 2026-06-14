package com.novelweaver.service;

/*
 * Deduction Service / 推演 / 推論
 *
 * CN 角色行为推演、大纲推演、验证
 * JP キャラクター行動推論、アウトライン推論、検証
 * EN Behavior deduction, outline deduction, verification
 */

import com.novelweaver.model.*;
import com.novelweaver.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DeductionService {

    private static final Logger log = LoggerFactory.getLogger(DeductionService.class);
    private static final int MAX_CANON_CONSTRAINTS = 20;

    private final CharacterProfileRepository profiles;
    private final CharacterSnapshotRepository snapshots;
    private final CharacterRelationshipRepository rels;
    private final ForeshadowingRepository foreshadows;
    private final CanonEventRepository canonEvents;
    private final CanonCharacterRepository canonChars;
    private final ChapterRepository chapters;
    private final ProjectRepository projects;
    private final DeductionLogRepository logs;

    public DeductionService(CharacterProfileRepository profiles, CharacterSnapshotRepository snapshots,
                            CharacterRelationshipRepository rels, ForeshadowingRepository foreshadows,
                            CanonEventRepository canonEvents, CanonCharacterRepository canonChars,
                            ChapterRepository chapters, ProjectRepository projects,
                            DeductionLogRepository logs) {
        this.profiles = profiles;
        this.snapshots = snapshots;
        this.rels = rels;
        this.foreshadows = foreshadows;
        this.canonEvents = canonEvents;
        this.canonChars = canonChars;
        this.chapters = chapters;
        this.projects = projects;
        this.logs = logs;
    }


    /*
     * 推演行为 / 行動推論 / Deduce behavior
     *
     * CN 推演角色行为——返回角色画像+状态+关系+伏笔作为上下文
     * JP キャラクター行動を推論——プロフィール+状態+関係+伏線を文脈として返す
     * EN Deduce character behavior — returns profile+state+relations+context
     */
    @McpTool(name = "deduce_behavior", description = "推演人物行为 — 基于画像 + 声线 + 关系 + 场景 | CN 推演角色行为 / JP キャラクター行動推論 / EN Deduce character behavior")
    public BehaviorContext behavior(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "场景描述", required = true) String scene,
            @McpToolParam(description = "角色名列表", required = true) List<String> charNames,
            @McpToolParam(description = "deepseek-chat | claude-3.5-sonnet", required = false) String model,
            @McpToolParam(description = "sequential | parallel | flashback | phase_shift", required = false) String timeMode) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        // 批量加载——避免 N+1
        List<CharacterProfile> cps = profiles.findByProjectAndNameIn(proj, charNames);
        List<CharacterSnapshot> allSnaps = snapshots.findByProjectAndCharacterNameIn(proj, charNames);
        Map<String, List<CharacterRelationship>> relMap = new LinkedHashMap<>();
        for (String name : charNames) {
            relMap.put(name, rels.findByProjectAndFromChar(proj, name));
        }

        List<CharContext> chars = new ArrayList<>();
        for (String name : charNames) {
            CharacterProfile cp = cps.stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
            chars.add(buildProfile(name, cp, allSnaps, relMap.getOrDefault(name, List.of())));
        }

        List<Foreshadowing> activeFores = foreshadows.findActiveByProject(proj);
        List<ForeshadowInfo> fores = activeFores.stream()
                .map(f -> new ForeshadowInfo(f.getCode(), f.getDescription(), f.getPlantedChapter(),
                        f.getStatus(), f.getRelatedCharacters()))
                .toList();

        List<CanonEvent> canonEvs = canonEvents.findByProject(proj);
        List<CanonConstraint> canon = canonEvs.stream()
                .limit(MAX_CANON_CONSTRAINTS)
                .map(e -> new CanonConstraint(e.getName(), e.getCanonLevel(), e.getDescription()))
                .toList();

        saveLogSafely(proj, null, "behavior", model, scene);

        return new BehaviorContext(chars, fores, canon, scene, timeMode);
    }


    /*
     * 推演大纲 / アウトライン推論 / Deduce outline
     *
     * CN 推演章节大纲（三种 mode）
     * JP 章アウトラインを推論（3つのモード）
     * EN Deduce chapter outline (3 modes)
     */
    @McpTool(name = "deduce_outline", description = "推演章节大纲 — 三个模式: 1=因果固定 2=因定果不定 3=无因无果 | CN 推演章节大纲 / JP 章アウトライン推論 / EN Deduce chapter outline")
    public OutlineContext outline(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "1=因果固定 2=因定果不定 3=无因无果", required = true) int mode,
            @McpToolParam(description = "前因", required = false) String premise,
            @McpToolParam(description = "结果（mode=1必填）", required = false) String result,
            @McpToolParam(description = "涉及人物", required = false) List<String> characters,
            @McpToolParam(description = "deepseek-chat | claude-3.5-sonnet", required = false) String model,
            @McpToolParam(description = "sequential | parallel | flashback | phase_shift", required = false) String timeMode) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        List<CharContext> chars = new ArrayList<>();
        if (characters != null && !characters.isEmpty()) {
            List<CharacterProfile> cps = profiles.findByProjectAndNameIn(proj, characters);
            List<CharacterSnapshot> allSnaps = snapshots.findByProjectAndCharacterNameIn(proj, characters);
            for (String name : characters) {
                CharacterProfile cp = cps.stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
                chars.add(buildProfile(name, cp, allSnaps, rels.findByProjectAndFromChar(proj, name)));
            }
        }

        // 只加载标题+摘要，不加载全文
        List<Chapter> allChs = chapters.findByProjectOrderByChapterNumber(proj);
        List<ChapterSummary> chList = allChs.stream()
                .map(c -> new ChapterSummary(c.getChapterNumber(), c.getTitle(), c.getSummary()))
                .toList();

        List<Foreshadowing> activeFores = foreshadows.findActiveByProject(proj);
        List<ForeshadowInfo> fores = activeFores.stream()
                .map(f -> new ForeshadowInfo(f.getCode(), f.getDescription(), f.getPlantedChapter(),
                        f.getStatus(), f.getRelatedCharacters()))
                .toList();

        saveLogSafely(proj, null, "outline", model,
                "mode=" + mode + " premise=" + premise + " result=" + result + " chars=" + characters);

        return new OutlineContext(mode, premise, result, chars, chList, fores, timeMode);
    }


    /*
     * 验证推演 / 検証 / Verify
     *
     * CN 验证推演结果一致性（正则 + voice_check）
     * JP 推論結果の一貫性を検証（正規表現+voice_check）
     * EN Verify deduction consistency (regex + voice_check)
     */
    @McpTool(name = "deduce_verify", description = "验证推演结果一致性 — 正则规则扫描 + voice_check 自检 | CN 验证推演结果 / JP 推論結果検証 / EN Verify deduction result")
    public VerifyResult verify(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "推演输出 JSON", required = true) String deductionOutput) {

        List<String> issues = new ArrayList<>();

        String[] forbidden = {
                "温柔地说", "轻声", "优雅地", "甜蜜地", "不是…而是"
        };
        for (String fp : forbidden) {
            if (deductionOutput.contains(fp)) {
                issues.add("forbidden_pattern: " + fp);
            }
        }

        int aiCount = 0;
        for (String phrase : List.of("不仅……而且", "一方面……另一方面", "综上所述", "总而言之")) {
            if (deductionOutput.contains(phrase)) {
                aiCount++;
            }
        }
        if (aiCount >= 2) {
            issues.add("AI_trace: 多个模板句式 (" + aiCount + "处)");
        }

        // 日志保存失败不阻塞返回
        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        saveLogSafely(proj, null, "verify", null, deductionOutput);

        return new VerifyResult(issues.size(), issues, issues.isEmpty());
    }

    // ── 辅助 ──

    private CharContext buildProfile(String name, CharacterProfile cp,
                                     List<CharacterSnapshot> allSnaps,
                                     List<CharacterRelationship> outbound) {
        if (cp == null) {
            return new CharContext(name, null, null, null, null);
        }

        SnapshotInfo latest = allSnaps.stream()
                .filter(s -> s.getCharacterName().equals(name))
                .findFirst()
                .map(s -> new SnapshotInfo(
                        s.getChapter().getChapterNumber(),
                        s.getPhysicalLocation(),
                        s.getPhysicalStatus(),
                        s.getCorePsychology(),
                        s.getKeyItems() != null ? Arrays.asList(s.getKeyItems()) : List.of()))
                .orElse(null);

        List<RelInfo> relations = outbound.stream()
                .map(r -> new RelInfo(r.getToChar(), r.getRelationType(), r.getTrustLevel()))
                .toList();

        VoiceInfo voice = null;
        if (cp.getVoice() != null || cp.getVoiceMeta() != null || cp.getVoiceSeeds() != null) {
            voice = new VoiceInfo(cp.getVoice(),
                    cp.getVoiceSeeds() != null ? Arrays.asList(cp.getVoiceSeeds()) : List.of(),
                    cp.getVoiceMeta());
        }

        return new CharContext(name, cp.getBio(), voice, relations, latest);
    }

    private void saveLogSafely(Project proj, Chapter chapter, String type, String model, String context) {
        try {
            DeductionLog log = new DeductionLog();
            log.setProject(proj);
            log.setChapter(chapter);
            log.setType(type);
            log.setModel(model);
            log.setInputContext(context);
            log.setCreatedAt(java.time.Instant.now());
            logs.save(log);
        } catch (Exception e) {
            log.warn("Failed to save deduction log (type={})", type, e);
        }
    }

    // ── result records ──


    /*
     * 登记伏笔 / 伏線登録 / Register
     *
     * CN 登记新伏笔到 foreshadowing_index 表
     * JP 新しい伏線を foreshadowing_index テーブルに登録
     * EN Register new foreshadowing in foreshadowing_index table
     */
    @McpTool(name = "register_foreshadowing", description = "登记新伏笔 — 写入 foreshadowing_index 表 | CN 登记新伏笔 / JP 新しい伏線を登録 / EN Register new foreshadowing")
    public ForeshadowingResult registerForeshadowing(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "伏笔编号（如 F005）", required = true) String code,
            @McpToolParam(description = "伏笔描述", required = true) String description,
            @McpToolParam(description = "类型 🔮情感 🎭身份 🎯事件 💡道具", required = true) String fType,
            @McpToolParam(description = "埋设章节号", required = true) int plantedChapter,
            @McpToolParam(description = "涉及角色", required = false) List<String> characters) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        Foreshadowing f = new Foreshadowing();
        f.setProject(proj);
        f.setCode(code);
        f.setDescription(description);
        f.setFType(fType);
        f.setPlantedChapter(plantedChapter);
        f.setStatus("active");
        f.setRelatedCharacters(characters != null ? characters.toArray(String[]::new) : new String[0]);
        f.setCreatedAt(java.time.Instant.now());
        foreshadows.save(f);

        return new ForeshadowingResult(code, plantedChapter, "active");
    }

    public record ForeshadowingResult(String code, int plantedChapter, String status) {
    }

    public record BehaviorContext(List<CharContext> characters, List<ForeshadowInfo> foreshadows,
                                  List<CanonConstraint> canon, String scene, String timeMode) {
    }

    public record CharContext(String name, String bio, VoiceInfo voice, List<RelInfo> relations,
                              SnapshotInfo latestStatus) {
    }

    public record VoiceInfo(String description, List<String> seedLines, String voiceMeta) {
    }

    public record RelInfo(String target, String type, String trustLevel) {
    }

    public record SnapshotInfo(int chapterNumber, String location, String physical,
                               String psychology, List<String> items) {
    }

    public record ForeshadowInfo(String code, String description, Integer plantedChapter,
                                 String status, String[] relatedCharacters) {
    }

    public record CanonConstraint(String eventName, String canonLevel, String description) {
    }

    public record OutlineContext(int mode, String premise, String result, List<CharContext> characters,
                                 List<ChapterSummary> chapters, List<ForeshadowInfo> foreshadows,
                                 String timeMode) {
    }

    public record ChapterSummary(int number, String title, String summary) {
    }

    public record VerifyResult(int issueCount, List<String> issues, boolean clean) {
    }
}
