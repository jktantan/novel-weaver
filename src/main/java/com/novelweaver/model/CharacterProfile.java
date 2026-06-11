package com.novelweaver.model;

/*
 * Character Profile / 人物画像 / キャラクタープロフィール
 *
 * CN 人物画像——性格特征、声线约束、当前状态
 * JP キャラクタープロフィール——性格特性、声色制約、現在の状態
 * EN Character profile — personality traits, voice constraints, status
 */

import com.novelweaver.type.PgVectorType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "character_profiles", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "name"})
}, indexes = {
        @Index(name = "idx_profiles_project", columnList = "project_id")
})
@Getter
@Setter
public class CharacterProfile {

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
    private String nameJp;
    @Column
    private String nameEn;
    @Column(columnDefinition = "text[]")
    private String[] aliases;
    @Column
    private String type;
    @Column(columnDefinition = "text")
    private String bio;
    @Column(columnDefinition = "text")
    private String profileFile;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String traits;
    @Column(columnDefinition = "text")
    private String voice;
    @Column(columnDefinition = "text[]")
    private String[] voiceSeeds;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String voiceMeta;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String status;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String basicInfo;
    @Column(columnDefinition = "text[]")
    private String[] personalityTraits;

    @Column(columnDefinition = "vector(1024)")
    @Type(PgVectorType.class)
    private String embedding;

    @Column(updatable = false)
    private Instant createdAt;
    @Column
    private Instant updatedAt;
}
