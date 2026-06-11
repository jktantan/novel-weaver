package com.novelweaver.model;

/*
 * Timeline Event / 时间线事件 / タイムラインイベント
 *
 * CN 时间线事件——绝对顺序+叙述顺序+日期标签
 * JP タイムラインイベント——絶対順序+叙述順序+日付ラベル
 * EN Timeline events — absolute+narrative order + date label
 */

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "timeline_events", indexes = @Index(name = "idx_timeline_event_project", columnList = "project_id"))
@Getter
@Setter
public class TimelineEvent {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timeline_id", nullable = false)
    private Timeline timeline;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;
    @Column(nullable = false)
    private String name;
    @Column
    private Integer absoluteOrder;
    @Column
    private Integer narrativeOrder;
    @Column(columnDefinition = "text")
    private String description;
    @Column(columnDefinition = "text")
    private String dateLabel;
    @Column
    private Boolean isCanon;
    @Column(updatable = false)
    private Instant createdAt;
}
