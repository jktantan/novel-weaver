package com.novelweaver.model;

/*
 * Canon Event / 正典事件 / 正典イベント
 *
 * CN 正典事件——从原作提取的不可变更事实
 * JP 正典イベント——原作から抽出した不変事実
 * EN Canon events — immutable facts from source
 */

import com.novelweaver.type.PgVectorType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "canon_events", indexes = @Index(name = "idx_canon_event_project", columnList = "project_id"))
@Getter
@Setter
public class CanonEvent {
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private CanonSource source;
    @Column(nullable = false)
    private String name;
    @Column
    private String timelinePos;
    @Column(columnDefinition = "text")
    private String description;
    @Column(columnDefinition = "text")
    private String dateLabel;
    @Column(columnDefinition = "text[]")
    private String[] participants;
    @Column(length = 5)
    private String canonLevel;
    @Column(columnDefinition = "vector(1024)")
    @Type(PgVectorType.class)
    private String embedding;
    @Column
    private Boolean verified;
    @Column(updatable = false)
    private Instant createdAt;
}
