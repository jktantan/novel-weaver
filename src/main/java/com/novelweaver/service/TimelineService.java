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

        // Neo4j: create :Timeline node
        try {
            neo4j.query("""
                            MERGE (t:Timeline {project_id: $pid, name: $name})
                            SET t.type = $type
                            """)
                    .bind(projectId).to("pid")
                    .bind(name).to("name")
                    .bind(tl.getType()).to("type")
                    .run();
        } catch (Exception e) {
            log.warn("Neo4j timeline node creation failed for {}/{}", projectId, name, e);
        }

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
            @McpToolParam(description = "描述", required = false) String description,
            @McpToolParam(description = "状态：planned(计划)/realized(已实现)/modified(被修改)/skipped(被跳过)，默认 planned", required = false) String status,
            @McpToolParam(description = "关联的计划事件ID——当本条是对某个计划事件的实现或修改时", required = false) String plannedEventId,
            @McpToolParam(description = "重要性：mandatory(必须发生)/important(重要)/optional(可选)，默认 important", required = false) String criticality,
            @McpToolParam(description = "时间灵活性：fixed(固定位置)/flexible(可提前推迟)/anytime(任意)，默认 flexible", required = false) String timeFlexibility) {

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
        ev.setStatus(status != null ? status : "planned");
        if (plannedEventId != null && !plannedEventId.isBlank()) {
            ev.setPlannedEventId(UUID.fromString(plannedEventId));
        }
        ev.setCriticality(criticality != null ? criticality : "important");
        ev.setTimeFlexibility(timeFlexibility != null ? timeFlexibility : "flexible");
        ev.setIsCanon(false);
        ev.setCreatedAt(Instant.now());
        events.save(ev);

        // If this event realizes a planned event, update the plan's status
        if (plannedEventId != null && !plannedEventId.isBlank()) {
            TimelineEvent plan = events.findById(UUID.fromString(plannedEventId)).orElse(null);
            if (plan != null && "planned".equals(plan.getStatus())) {
                plan.setStatus("realized");
                events.save(plan);
            }
        }

        // Neo4j: create :TimelineEvent node linked to :Timeline
        try {
            neo4j.query("""
                            MATCH (t:Timeline {project_id: $pid, name: $tlName})
                            MERGE (e:TimelineEvent {project_id: $pid, name: $name,
                                    timeline: $tlName})
                            SET e.absoluteOrder = $absOrder, e.narrativeOrder = $narOrder,
                                e.dateLabel = $dateLabel
                            MERGE (e)-[:OCCURS_IN]->(t)
                            """)
                    .bind(projectId).to("pid")
                    .bind(tl.getName()).to("tlName")
                    .bind(name).to("name")
                    .bind(absoluteOrder != null ? absoluteOrder : -1).to("absOrder")
                    .bind(narrativeOrder != null ? narrativeOrder : -1).to("narOrder")
                    .bind(dateLabel != null ? dateLabel : "").to("dateLabel")
                    .run();
        } catch (Exception e) {
            log.warn("Neo4j timeline event creation failed for {}/{}", projectId, name, e);
        }

        return new TimelineEventAddResult("ok", name, tl.getName());
    }


    /*
     * 更新事件状态 / 更新 / Update
     *
     * CN 更新时间线事件状态——计划事件被实现/修改/跳过时，标记实际走向
     * JP タイムラインイベント状態更新——計画が実現/変更/スキップされた時、実際の展開をマーク
     * EN Update timeline event status — mark plan as realized/modified/skipped
     */
    @McpTool(name = "timeline_event_update", description = "更新时间线事件状态——标记计划事件的实际走向(realized/modified/skipped)，可关联实际发生的章节和替代事件 | CN 更新事件状态 / JP イベント状態更新 / EN Update event status")
    @Transactional
    public TimelineEventUpdateResult updateEvent(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "事件ID", required = true) String eventId,
            @McpToolParam(description = "新状态：realized(已实现)/modified(被修改)/skipped(被跳过)", required = true) String status,
            @McpToolParam(description = "实际发生在哪一章", required = false) Integer chapterNumber,
            @McpToolParam(description = "实际描述（与计划不同时）", required = false) String actualDescription,
            @McpToolParam(description = "偏离原因", required = false) String divergenceReason,
            @McpToolParam(description = "替代此计划的实现事件ID（当 status=modified 时，指向实际发生的新事件）", required = false) String realizedByEventId) {

        UUID pid = UUID.fromString(projectId);
        if (!projects.existsById(pid)) {
            throw new IllegalArgumentException("Project not found: " + projectId);
        }

        TimelineEvent ev = events.findById(UUID.fromString(eventId))
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        String oldStatus = ev.getStatus();
        ev.setStatus(status);

        // Link to chapter if specified
        if (chapterNumber != null) {
            // Chapter linkage via PG — store in description for now, chapter_id is optional
            // Update Neo4j to link the event to the chapter node
            try {
                neo4j.query("""
                                MERGE (ch:Chapter {project_id: $pid, number: $num})
                                """)
                        .bind(projectId).to("pid")
                        .bind(chapterNumber).to("num")
                        .run();
            } catch (Exception e) {
                log.warn("Neo4j chapter MERGE failed for event {}/{}", projectId, ev.getName(), e);
            }
        }

        // Store actual description and divergence reason in the event's description
        if (actualDescription != null && !actualDescription.isBlank()) {
            ev.setDescription(actualDescription);
        }

        // If this plan was replaced by another event, link them
        if (realizedByEventId != null && !realizedByEventId.isBlank()) {
            TimelineEvent realized = events.findById(UUID.fromString(realizedByEventId)).orElse(null);
            if (realized != null && realized.getPlannedEventId() == null) {
                realized.setPlannedEventId(ev.getId());
                events.save(realized);
            }
        }

        ev.setCreatedAt(ev.getCreatedAt()); // keep original
        events.save(ev);

        return new TimelineEventUpdateResult("ok", ev.getName(), oldStatus, status,
                chapterNumber != null ? chapterNumber : 0,
                divergenceReason);
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

        // 强制事件状态检查
        for (TimelineEvent ev : all) {
            if ("mandatory".equals(ev.getCriticality()) && "skipped".equals(ev.getStatus())) {
                warnings.add("!! 强制事件被跳过: " + ev.getName()
                        + " (" + ev.getCriticality() + ")"
                        + (ev.getTimeline() != null ? " [时间线: " + ev.getTimeline().getName() + "]" : "")
                        + " — 跳过可能导致故事逻辑崩塌");
            }
            if ("mandatory".equals(ev.getCriticality()) && "modified".equals(ev.getStatus())) {
                warnings.add("⚠ 强制事件被修改: " + ev.getName()
                        + " (" + ev.getCriticality() + ")"
                        + (ev.getTimeline() != null ? " [时间线: " + ev.getTimeline().getName() + "]" : "")
                        + " — 请确认修改后仍满足故事逻辑");
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
            log.warn("Neo4j cycle detection skipped (Neo4j unavailable for project {})", projectId);
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

        // Neo4j: create relationship between :Timeline nodes
        try {
            neo4j.query("""
                            MATCH (a:Timeline {project_id: $pid, name: $fromName})
                            MATCH (b:Timeline {project_id: $pid, name: $toName})
                            MERGE (a)-[:LINKS_TO {type: $linkType}]->(b)
                            """)
                    .bind(projectId).to("pid")
                    .bind(from.getName()).to("fromName")
                    .bind(to.getName()).to("toName")
                    .bind(linkType).to("linkType")
                    .run();
        } catch (Exception e) {
            log.warn("Neo4j timeline link creation failed for {}/{}->{}", projectId, from.getName(), to.getName(), e);
        }

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
            @McpToolParam(description = "时间线ID（不传则查项目的全部关联）", required = false) String timelineId,
            @McpToolParam(description = "返回数量上限（默认50）", required = false) Integer limit) {

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

        int lim = limit != null ? Math.max(1, limit) : 50;
        List<Map<String, Object>> result = new ArrayList<>();
        int count = 0;
        for (TimelineLink l : allLinks) {
            if (count >= lim) break;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", l.getId().toString());
            entry.put("from_timeline", l.getFromTimeline().getName());
            entry.put("to_timeline", l.getToTimeline().getName());
            entry.put("link_type", l.getLinkType());
            entry.put("from_absolute_order", l.getFromAbsoluteOrder());
            entry.put("to_absolute_order", l.getToAbsoluteOrder());
            entry.put("description", l.getDescription());
            result.add(entry);
            count++;
        }

        return new TimelineLinkQueryResult("ok", result);
    }

    // ── result records ──

    public record TimelineCreateResult(String status, String timelineId, String name) {
    }

    public record TimelineEventAddResult(String status, String eventName, String timelineName) {
    }

    public record TimelineEventUpdateResult(String status, String eventName, String oldStatus,
                                            String newStatus, int chapter,
                                            String divergenceReason) {
    }

    public record TimelineCheckResult(int conflicts, List<String> details, String note) {
    }

    public record TimelineLinkCreateResult(String status, String linkId, String fromTimeline, String toTimeline,
                                           String linkType) {
    }

    public record TimelineLinkQueryResult(String status, List<Map<String, Object>> links) {
    }
}
