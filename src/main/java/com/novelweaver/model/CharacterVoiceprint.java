package com.novelweaver.model;

/*
 * Voiceprint / 声纹 / 声紋
 *
 * CN 声纹样本——标志性台词及其向量
 * JP 声紋サンプル——特徴的台詞とベクトル
 * EN Voiceprint samples — signature lines + vectors
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
@Table(name = "character_voiceprints", indexes = {
        @Index(name = "idx_voiceprints_char", columnList = "character_id"),
        @Index(name = "idx_voiceprints_project", columnList = "project_id")
})
@Getter
@Setter
public class CharacterVoiceprint {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private CharacterProfile character;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;
    @Column(nullable = false, columnDefinition = "text")
    private String dialogue;
    @Column(length = 20)
    private String source;
    @Column(columnDefinition = "vector(1024)")
    @Type(PgVectorType.class)
    private String embedding;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String meta;
    @Column(updatable = false)
    private Instant createdAt;
}
