package com.novelweaver.service;

/*
 * Timeline Service / 时间线管理 / タイムライン管理
 *
 * CN 时间线创建、事件添加、矛盾检测、关联管理
 * JP タイムライン作成、イベント追加、矛盾検出、関連管理
 * EN Timeline create, event add, conflict check, link management
 */

import com.novelweaver.model.Project;
import com.novelweaver.model.Timeline;
import com.novelweaver.model.TimelineEvent;
import com.novelweaver.model.TimelineLink;
import com.novelweaver.repository.ProjectRepository;
import com.novelweaver.repository.TimelineEventRepository;
import com.novelweaver.repository.TimelineLinkRepository;
import com.novelweaver.repository.TimelineRepository;
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
public class TimelineService {

    private static final Logger log = LoggerFactory.getLogger(TimelineService.class);

    private final TimelineRepository timelines;
    private final TimelineEventRepository events;
    private final TimelineLinkRepository links;
    private final ProjectRepository projects;
    private final Neo4jClient neo4j;

    public TimelineService(TimelineRepository timelines, TimelineEventRepository events,
                           TimelineLinkRepository links,
                           ProjectRepository projects, Neo4jClient neo4j) {
        this.timelines = timelines;
        this.events = events;
        this.links = links;
        this.projects = projects;
        this.neo4j = neo4j;
    }


    /*
     * 创建时间线 / 作成 / Create
     *
     * CN 创建时间线（主线/回忆/分支/闭环/平行）
     * JP タイムラインを作成（本筋/回想/分岐/ループ/並行）
     * EN Create timeline (main/flashback/branch/loop/alternative)
     */
    @McpTool(name = "timeline_create", description = "创建时间线 | CN 创建时间线 / JP タイムライン作成 / EN Create timeline")
    @Transactional
    public TimelineCreateResult create(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "时间线名称", required = true) String name,
            @McpToolParam(description = "main | flashback | branch | loop | alternative", required = false) String type) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        Timeline tl = new Timeline();
        tl.setProject(proj);
        tl.setName(name);
        tl.setType(type != null ? type : "main");
        tl.setCreatedAt(Instant.now());
        tl = timelines.save(tl);

        return new TimelineCreateResult("ok", tl.getId().toString(), name);
    }


    /*
     * 添加事件 / 追加 / Add event
     *
     * CN 添加时间线事件，支持日期标签
     * JP タイムラインイベントを追加、日付ラベル対応
     * EN Add timeline event with optional date label
     */
    @McpTool(name = "timeline_event_add", description = "添加时间线事件 | CN 添加时间线事件 / JP タイムラインイベント追加 / EN Add timeline event")
    @Transactional
    public TimelineEventAddResult addEvent(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "时间线ID", required = true) String timelineId,
            @McpToolParam(description = "事件名", required = true) String name,
            @McpToolParam(description = "时间标签（如 '2003年' / '星历元年' — 不填则只靠 order 排序）", required = false) String dateLabel,
            @McpToolParam(description = "实际时间顺序", required = false) Integer absoluteOrder,
            @McpToolParam(description = "叙述顺序", required = false) Integer narrativeOrder,
            @McpToolParam(description = "描述", required = false) String description) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        Timeline tl = timelines.findById(UUID.fromString(timelineId))
                .orElseThrow(() -> new IllegalArgumentException("Timeline not found: " + timelineId));

        TimelineEvent ev = new TimelineEvent();
        ev.setProject(proj);
        ev.setTimeline(tl);
        ev.setName(name);
        ev.setDateLabel(dateLabel);
        ev.setAbsoluteOrder(absoluteOrder);
        ev.setNarrativeOrder(narrativeOrder);
        ev.setDescription(description);
        ev.setCreatedAt(Instant.now());
        events.save(ev);

        return new TimelineEventAddResult("ok", name, tl.getName());
    }


    /*
     * 检查矛盾 / チェック / Check
     *
     * CN 检查时间线矛盾——顺序冲突 + Neo4j 环检测
     * JP タイムライン矛盾チェック——順序競合+Neo4j循環検出
     * EN Check timeline conflicts — order conflicts + Neo4j cycle detection
     */
    @McpTool(name = "timeline_check", description = "检查时间线矛盾 — 规则检查 + Neo4j 图遍历 | CN 检查时间线矛盾 / JP タイムライン矛盾チェック / EN Check timeline conflicts")
    public TimelineCheckResult check(
            @McpToolParam(description = "项目ID", required = true) String projectId) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        List<String> warnings = new ArrayList<>();
        List<TimelineEvent> all = events.findByProject(proj);

        // absolute_order 重复
        Map<Integer, List<String>> byAbsOrder = new LinkedHashMap<>();
        for (TimelineEvent ev : all) {
            if (ev.getAbsoluteOrder() != null) {
                byAbsOrder.computeIfAbsent(ev.getAbsoluteOrder(), k -> new ArrayList<>())
                        .add(ev.getName());
            }
        }
        for (var entry : byAbsOrder.entrySet()) {
            if (entry.getValue().size() > 1) {
                warnings.add("absolute_order=" + entry.getKey()
                        + " 被多个事件使用: " + entry.getValue());
            }
        }

        // 叙述顺序逆转
        List<TimelineEvent> sorted = all.stream()
                .filter(e -> e.getAbsoluteOrder() != null && e.getNarrativeOrder() != null)
                .sorted(Comparator.comparingInt(TimelineEvent::getAbsoluteOrder))
                .toList();
        for (int i = 1; i < sorted.size(); i++) {
            TimelineEvent prev = sorted.get(i - 1);
            TimelineEvent curr = sorted.get(i);
            if (prev.getNarrativeOrder() > curr.getNarrativeOrder()) {
                warnings.add(prev.getName() + "(abs=" + prev.getAbsoluteOrder()
                        + ",nar=" + prev.getNarrativeOrder() + ") → "
                        + curr.getName() + "(abs=" + curr.getAbsoluteOrder()
                        + ",nar=" + curr.getNarrativeOrder() + ") 叙述顺序逆转");
            }
        }

        // Neo4j 环检测
        try {
            var cycles = neo4j.query("""
                            MATCH p = (start:Chapter {project_id: $pid})-[:NEXT*2..]->(start)
                            RETURN length(p) AS cycleLen, [n IN nodes(p) | n.number] AS cyclePath
                            LIMIT 1
                            """)
                    .bind(projectId).to("pid")
                    .fetch()
                    .all();

            for (var row : cycles) {
                warnings.add("Neo4j: Chapter :NEXT 链检测到环: " + row.get("cyclePath")
                        + " (length=" + row.get("cycleLen") + ")");
            }
        } catch (Exception e) {
            log.debug("Neo4j cycle detection skipped (Neo4j unavailable for project {})", projectId);
        }

        return new TimelineCheckResult(warnings.size(), warnings, null);
    }


    /*
     * 关联时间线 / 関連付け / Link
     *
     * CN 连接两条时间线（回忆/分支/跳转）
     * JP 2つのタイムラインを関連付け（回想/分岐/跳躍）
     * EN Link two timelines (flashback/branch/jump)
     */
    @McpTool(name = "timeline_link_create", description = "连接两条时间线——表示回忆/分支/时间跳转关系 | CN 连接时间线 / JP タイムライン関連付け / EN Link timelines")
    @Transactional
    public TimelineLinkCreateResult linkCreate(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "源时间线ID", required = true) String fromTimelineId,
            @McpToolParam(description = "目标时间线ID", required = true) String toTimelineId,
            @McpToolParam(description = "链接类型 flashback_of(回忆) / alternative_to(平行分支) / time_jump_from(时间跳转)", required = true) String linkType,
            @McpToolParam(description = "源事件顺序号（从哪个位置跳转）", required = false) Integer fromAbsoluteOrder,
            @McpToolParam(description = "目标事件顺序号（跳转到哪个位置）", required = false) Integer toAbsoluteOrder,
            @McpToolParam(description = "描述——为什么发生这个跳转", required = false) String description) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        Timeline from = timelines.findById(UUID.fromString(fromTimelineId))
                .orElseThrow(() -> new IllegalArgumentException("Timeline not found: " + fromTimelineId));
        Timeline to = timelines.findById(UUID.fromString(toTimelineId))
                .orElseThrow(() -> new IllegalArgumentException("Timeline not found: " + toTimelineId));

        TimelineLink link = new TimelineLink();
        link.setProject(proj);
        link.setFromTimeline(from);
        link.setToTimeline(to);
        link.setLinkType(linkType);
        link.setFromAbsoluteOrder(fromAbsoluteOrder);
        link.setToAbsoluteOrder(toAbsoluteOrder);
        link.setDescription(description);
        link.setCreatedAt(Instant.now());
        links.save(link);

        return new TimelineLinkCreateResult("ok", link.getId().toString(),
                from.getName(), to.getName(), linkType);
    }


    /*
     * 查询关联 / 照会 / Query
     *
     * CN 查询某时间线的所有关联
     * JP タイムラインの全関連を照会
     * EN Query all links for a timeline
     */
    @McpTool(name = "timeline_link_query", description = "查询某条时间线的所有关联（回忆/分支/跳转） | CN 查询时间线关联 / JP タイムライン関連照会 / EN Query timeline links")
    public TimelineLinkQueryResult linkQuery(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "时间线ID（不传则查项目的全部关联）", required = false) String timelineId) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        List<TimelineLink> allLinks;
        if (timelineId != null && !timelineId.isBlank()) {
            UUID tid = UUID.fromString(timelineId);
            allLinks = new ArrayList<>();
            allLinks.addAll(links.findByFromTimelineId(tid));
            allLinks.addAll(links.findByToTimelineId(tid));
        } else {
            allLinks = links.findByProject(proj);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (TimelineLink l : allLinks) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", l.getId().toString());
            entry.put("from_timeline", l.getFromTimeline().getName());
            entry.put("to_timeline", l.getToTimeline().getName());
            entry.put("link_type", l.getLinkType());
            entry.put("from_absolute_order", l.getFromAbsoluteOrder());
            entry.put("to_absolute_order", l.getToAbsoluteOrder());
            entry.put("description", l.getDescription());
            result.add(entry);
        }

        return new TimelineLinkQueryResult("ok", result);
    }

    // ── result records ──

    public record TimelineCreateResult(String status, String timelineId, String name) {
    }

    public record TimelineEventAddResult(String status, String eventName, String timelineName) {
    }

    public record TimelineCheckResult(int conflicts, List<String> details, String note) {
    }

    public record TimelineLinkCreateResult(String status, String linkId, String fromTimeline, String toTimeline,
                                           String linkType) {
    }

    public record TimelineLinkQueryResult(String status, List<Map<String, Object>> links) {
    }
}
