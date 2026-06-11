package com.novelweaver.model;

/*
 * Timeline / 时间线 / タイムライン
 *
 * CN 时间线定义——支持多条并列
 * JP タイムライン定義——複数並行対応
 * EN Timeline definition — supports parallel timelines
 */

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "timelines")
@Getter
@Setter
public class Timeline {
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
    @Column
    private String type;
    @Column(columnDefinition = "text")
    private String description;
    @Column(updatable = false)
    private Instant createdAt;
}
