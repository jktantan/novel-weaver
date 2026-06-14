package com.novelweaver.model;

/*
 * Location / 地点 / 場所
 *
 * CN 地点档案——初始外观、变更历史、当前状态
 * JP 場所アーカイブ——初期外観、変更履歴、現在状態
 * EN Location archive — initial appearance, changes, current status
 */

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "locations", uniqueConstraints = {
}, indexes = {
        @Index(name = "idx_locations_project", columnList = "project_id")
})
@Getter
@Setter
public class Location {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "universe_id")
    private Universe universe;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String locationType;

    @Column(columnDefinition = "text")
    private String region;

    private Integer firstChapter;

    // ── 初始状态 ──

    @Column(columnDefinition = "text")
    private String canonDescription;

    @Column(columnDefinition = "text")
    private String actualAppearance;

    @Column(columnDefinition = "text")
    private String sensoryDetail;

    @Column(columnDefinition = "text")
    private String narrativeFunction;

    // ── 变更历史（JSON 数组） ──

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String changeLog;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String identity;

    @Column(columnDefinition = "text")
    private String currentStatus;

    @Column(updatable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;
}
