package com.novelweaver.model;

/*
 * Item / 物品 / アイテム
 *
 * CN 物品档案——来源、归属、用途、生命周期
 * JP アイテムアーカイブ——来歴、所有者、用途、ライフサイクル
 * EN Item archive — origin, ownership, usage, lifecycle
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
@Table(name = "items", uniqueConstraints = {
}, indexes = {
        @Index(name = "idx_items_project", columnList = "project_id")
})
@Getter
@Setter
public class Item {

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
    private String itemType;

    // ── 基本描述 ──

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String origin;

    @Column(columnDefinition = "text")
    private String significance;

    // ── 自定义属性（JSON） ──

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String properties;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String identity;

    // ── 当前持有者 / 位置（冗余字段，方便快速查询） ──

    @Column(columnDefinition = "text")
    private String currentHolder;

    @Column(columnDefinition = "text")
    private String currentLocation;

    @Column(columnDefinition = "text")
    private String currentStatus;

    private Integer firstChapter;

    // ── 归属变更历史（JSON 数组） ──

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String ownerHistory;

    // ── pgvector embedding（语义搜索） ──

    @Column(columnDefinition = "vector(1024)")
    private String embedding;

    @Column(updatable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;
}
