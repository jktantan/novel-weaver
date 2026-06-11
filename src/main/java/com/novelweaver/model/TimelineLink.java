package com.novelweaver.model;

/*
 * Timeline Link / 时间线关联 / タイムライン関連
 *
 * CN 时间线之间的关联——跳转、回忆、分支
 * JP タイムライン間の関連——跳躍、回想、分岐
 * EN Timeline links — jumps, flashbacks, branches
 */

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "timeline_links", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "from_timeline_id", "to_timeline_id"})
}, indexes = {
        @Index(name = "idx_timeline_links_project", columnList = "project_id"),
        @Index(name = "idx_timeline_links_from", columnList = "from_timeline_id"),
        @Index(name = "idx_timeline_links_to", columnList = "to_timeline_id")
})
@Getter
@Setter
public class TimelineLink {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_timeline_id", nullable = false)
    private Timeline fromTimeline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_timeline_id", nullable = false)
    private Timeline toTimeline;

    @Column(nullable = false)
    private String linkType;

    private Integer fromAbsoluteOrder;

    private Integer toAbsoluteOrder;

    @Column(columnDefinition = "text")
    private String description;

    @Column(updatable = false)
    private Instant createdAt;
}
