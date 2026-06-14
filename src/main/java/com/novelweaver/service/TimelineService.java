package com.novelweaver.service;

/*
 * Timeline Service / 时间线管理 / タイムライン管理
 *
 * CN 时间线创建、事件添加、矛盾检测、关联管理 — 图部分使用 ArcadeDB（物理租户）
 * JP タイムライン作成、イベント追加、矛盾検出、関連管理 — グラフは ArcadeDB
 * EN Timeline — graph via ArcadeDB (physical tenant)
 */

import com.arcadedb.remote.RemoteDatabase;
import com.novelweaver.config.ArcadeDBManager;
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
    private final ArcadeDBManager arcadeDB;

    public TimelineService(TimelineRepository timelines, TimelineEventRepository events,
                           TimelineLinkRepository links,
                           ProjectRepository projects, ArcadeDBManager arcadeDB) {
        this.timelines = timelines;
        this.events = events;
        this.links = links;
        this.projects = projects;
        this.arcadeDB = arcadeDB;
    }

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

        try (RemoteDatabase db = arcadeDB.open(projectId)) {
            db.command("cypher", "MERGE (t:Timeline {name: $name}) SET t.type = $type",
                    Map.of("name", name, "type", tl.getType()));
        } catch (Exception e) {
            log.warn("ArcadeDB timeline node creation failed for {}/{}", projectId, name, e);
        }

        return new TimelineCreateResult("ok", tl.getId().toString(), name);
    }

    @McpTool(name = "timeline_event_add", description = "添加时间线事件 | CN 添加时间线事件 / JP タイムラインイベント追加 / EN Add timeline event")
    @Transactional
    public TimelineEventAddResult addEvent(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "时间线ID", required = true) String timelineId,
            @McpToolParam(description = "事件名", required = true) String name,
            @McpToolParam(description = "时间标签", required = false) String dateLabel,
            @McpToolParam(description = "实际时间顺序", required = false) Integer absoluteOrder,
            @McpToolParam(description = "叙述顺序", required = false) Integer narrativeOrder,
            @McpToolParam(description = "描述", required = false) String description,
            @McpToolParam(description = "状态", required = false) String status,
            @McpToolParam(description = "关联的计划事件ID", required = false) String plannedEventId,
            @McpToolParam(description = "重要性", required = false) String criticality,
            @McpToolParam(description = "时间灵活性", required = false) String timeFlexibility) {

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

        if (plannedEventId != null && !plannedEventId.isBlank()) {
            TimelineEvent plan = events.findById(UUID.fromString(plannedEventId)).orElse(null);
            if (plan != null && "planned".equals(plan.getStatus())) {
                plan.setStatus("realized");
                events.save(plan);
            }
        }

        try (RemoteDatabase db = arcadeDB.open(projectId)) {
            db.command("cypher", """
                            MATCH (t:Timeline {name: $tlName})
                            MERGE (e:TimelineEvent {name: $name, timeline: $tlName})
                            SET e.absoluteOrder = $absOrder, e.narrativeOrder = $narOrder,
                                e.dateLabel = $dateLabel
                            MERGE (e)-[:OCCURS_IN]->(t)
                            """,
                    Map.of("tlName", tl.getName(), "name", name,
                            "absOrder", absoluteOrder != null ? absoluteOrder : -1,
                            "narOrder", narrativeOrder != null ? narrativeOrder : -1,
                            "dateLabel", dateLabel != null ? dateLabel : ""));
        } catch (Exception e) {
            log.warn("ArcadeDB timeline event creation failed for {}/{}", projectId, name, e);
        }

        return new TimelineEventAddResult("ok", name, tl.getName());
    }

    @McpTool(name = "timeline_event_update", description = "更新时间线事件状态 | CN 更新事件状态 / JP イベント状態更新 / EN Update event status")
    @Transactional
    public TimelineEventUpdateResult updateEvent(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "事件ID", required = true) String eventId,
            @McpToolParam(description = "新状态", required = true) String status,
            @McpToolParam(description = "实际发生在哪一章", required = false) Integer chapterNumber,
            @McpToolParam(description = "实际描述", required = false) String actualDescription,
            @McpToolParam(description = "偏离原因", required = false) String divergenceReason,
            @McpToolParam(description = "替代事件的ID", required = false) String realizedByEventId) {

        UUID pid = UUID.fromString(projectId);
        if (!projects.existsById(pid)) {
            throw new IllegalArgumentException("Project not found: " + projectId);
        }

        TimelineEvent ev = events.findById(UUID.fromString(eventId))
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        String oldStatus = ev.getStatus();
        ev.setStatus(status);

        if (chapterNumber != null) {
            try (RemoteDatabase db = arcadeDB.open(projectId)) {
                db.command("cypher", "MERGE (ch:Chapter {number: $num})",
                        Map.of("num", chapterNumber));
            } catch (Exception e) {
                log.warn("ArcadeDB chapter MERGE failed for event {}/{}", projectId, ev.getName(), e);
            }
        }

        if (actualDescription != null && !actualDescription.isBlank()) {
            ev.setDescription(actualDescription);
        }

        if (realizedByEventId != null && !realizedByEventId.isBlank()) {
            TimelineEvent realized = events.findById(UUID.fromString(realizedByEventId)).orElse(null);
            if (realized != null && realized.getPlannedEventId() == null) {
                realized.setPlannedEventId(ev.getId());
                events.save(realized);
            }
        }

        events.save(ev);

        return new TimelineEventUpdateResult("ok", ev.getName(), oldStatus, status,
                chapterNumber != null ? chapterNumber : 0, divergenceReason);
    }

    @McpTool(name = "timeline_check", description = "检查时间线矛盾 | CN 检查时间线矛盾 / JP タイムライン矛盾チェック / EN Check timeline conflicts")
    public TimelineCheckResult check(
            @McpToolParam(description = "项目ID", required = true) String projectId) {

        Project proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        List<String> warnings = new ArrayList<>();
        List<TimelineEvent> all = events.findByProject(proj);

        Map<Integer, List<String>> byAbsOrder = new LinkedHashMap<>();
        for (TimelineEvent ev : all) {
            if (ev.getAbsoluteOrder() != null) {
                byAbsOrder.computeIfAbsent(ev.getAbsoluteOrder(), k -> new ArrayList<>()).add(ev.getName());
            }
        }
        for (var entry : byAbsOrder.entrySet()) {
            if (entry.getValue().size() > 1) {
                warnings.add("absolute_order=" + entry.getKey() + " 被多个事件使用: " + entry.getValue());
            }
        }

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

        for (TimelineEvent ev : all) {
            if ("mandatory".equals(ev.getCriticality()) && "skipped".equals(ev.getStatus())) {
                warnings.add("!! 强制事件被跳过: " + ev.getName());
            }
            if ("mandatory".equals(ev.getCriticality()) && "modified".equals(ev.getStatus())) {
                warnings.add("⚠ 强制事件被修改: " + ev.getName());
            }
        }

        // ArcadeDB cycle detection
        try (RemoteDatabase db = arcadeDB.open(projectId)) {
            var result = db.query("cypher", """
                    MATCH p = (start:Chapter)-[:NEXT*2..]->(start)
                    RETURN length(p) AS cycleLen, [n IN nodes(p) | n.number] AS cyclePath
                    LIMIT 1
                    """, Map.of());
            while (result.hasNext()) {
                var row = result.next();
                warnings.add("ArcadeDB: Chapter :NEXT 链检测到环: " + row.getProperty("cyclePath")
                        + " (length=" + row.getProperty("cycleLen") + ")");
            }
        } catch (Exception e) {
            log.warn("ArcadeDB cycle detection skipped for project {}", projectId);
        }

        return new TimelineCheckResult(warnings.size(), warnings, null);
    }

    @McpTool(name = "timeline_link_create", description = "连接两条时间线 | CN 连接时间线 / JP タイムライン関連付け / EN Link timelines")
    @Transactional
    public TimelineLinkCreateResult linkCreate(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "源时间线ID", required = true) String fromTimelineId,
            @McpToolParam(description = "目标时间线ID", required = true) String toTimelineId,
            @McpToolParam(description = "链接类型", required = true) String linkType,
            @McpToolParam(description = "源事件顺序号", required = false) Integer fromAbsoluteOrder,
            @McpToolParam(description = "目标事件顺序号", required = false) Integer toAbsoluteOrder,
            @McpToolParam(description = "描述", required = false) String description) {

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

        try (RemoteDatabase db = arcadeDB.open(projectId)) {
            db.command("cypher", """
                            MATCH (a:Timeline {name: $fromName})
                            MATCH (b:Timeline {name: $toName})
                            MERGE (a)-[:LINKS_TO {type: $linkType}]->(b)
                            """,
                    Map.of("fromName", from.getName(), "toName", to.getName(), "linkType", linkType));
        } catch (Exception e) {
            log.warn("ArcadeDB timeline link failed for {}/{}->{}", projectId, from.getName(), to.getName(), e);
        }

        return new TimelineLinkCreateResult("ok", link.getId().toString(),
                from.getName(), to.getName(), linkType);
    }

    @McpTool(name = "timeline_link_query", description = "查询时间线关联 | CN 查询时间线关联 / JP タイムライン関連照会 / EN Query timeline links")
    public TimelineLinkQueryResult linkQuery(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "时间线ID", required = false) String timelineId,
            @McpToolParam(description = "返回数量上限", required = false) Integer limit) {

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

    public record TimelineCreateResult(String status, String timelineId, String name) {
    }

    public record TimelineEventAddResult(String status, String eventName, String timelineName) {
    }

    public record TimelineEventUpdateResult(String status, String eventName, String oldStatus,
                                            String newStatus, int chapter, String divergenceReason) {
    }

    public record TimelineCheckResult(int conflicts, List<String> details, String note) {
    }

    public record TimelineLinkCreateResult(String status, String linkId, String fromTimeline, String toTimeline,
                                           String linkType) {
    }

    public record TimelineLinkQueryResult(String status, List<Map<String, Object>> links) {
    }
}
