package com.novelweaver.model;

/*
 * Chapter Paragraph / 段落 / 段落
 *
 * CN 段落级语义搜索单元
 * JP 段落レベルの意味検索ユニット
 * EN Paragraph-level semantic search unit
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
@Table(name = "chapter_paragraphs", indexes = {
        @Index(name = "idx_paragraphs_chapter", columnList = "chapter_id"),
        @Index(name = "idx_paragraphs_project", columnList = "project_id"),
        @Index(name = "idx_paragraphs_scene", columnList = "scene_type"),
        @Index(name = "idx_paragraphs_pov", columnList = "pov_character")
})
@Getter
@Setter
public class ChapterParagraph {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private ChapterVersion version;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    @Column(nullable = false)
    private Integer seq;
    @Column(columnDefinition = "text")
    private String scene;
    @Column(columnDefinition = "text")
    private String content;
    @Column(columnDefinition = "text")
    private String povCharacter;
    @Column
    private String sceneType;
    @Column(columnDefinition = "vector(1024)")
    @Type(PgVectorType.class)
    private String embedding;
    @Column(updatable = false)
    private Instant createdAt;
    @Column
    private Instant updatedAt;
}
