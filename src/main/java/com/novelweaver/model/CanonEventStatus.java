package com.novelweaver.model;

/*
 * Canon Event Status / 正典走向 / 正典展開
 *
 * CN 同人小说中的正典事件走向追踪
 * JP 二次創作における正典イベントの展開追跡
 * EN Tracks canon events in fanfic
 */

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "canon_event_status", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "canon_event_id"})
}, indexes = {
        @Index(name = "idx_canon_evt_status_project", columnList = "project_id")
})
@Getter
@Setter
public class CanonEventStatus {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canon_event_id", nullable = false)
    private CanonEvent canonEvent;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "text")
    private String actualDescription;

    private Integer occurredInChapter;

    @Column(columnDefinition = "text")
    private String divergenceReason;

    @Column(updatable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;
}
