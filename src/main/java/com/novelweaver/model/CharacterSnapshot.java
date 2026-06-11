package com.novelweaver.model;

/*
 * Character Snapshot / 角色快照 / キャラクター状態
 *
 * CN 角色状态快照——每章结束后记录一次
 * JP キャラクター状態スナップショット——各章終了後記録
 * EN Character state snapshot — recorded after each chapter
 */

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "character_snapshots", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"chapter_id", "character_id"})
}, indexes = {
        @Index(name = "idx_snapshots_char", columnList = "character_id"),
        @Index(name = "idx_snapshots_chapter", columnList = "chapter_id"),
        @Index(name = "idx_snapshots_project", columnList = "project_id")
})
@Getter
@Setter
public class CharacterSnapshot {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private CharacterProfile character;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String characterName;

    @Column(columnDefinition = "text")
    private String physicalLocation;

    @Column(columnDefinition = "text")
    private String physicalStatus;

    @Column(columnDefinition = "text")
    private String corePsychology;

    @Column(columnDefinition = "text[]")
    private String[] keyItems;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(updatable = false)
    private Instant createdAt;
    @Column
    private Instant updatedAt;
}
