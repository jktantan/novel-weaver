package com.novelweaver.model;

/*
 * Chapter / 章节 / 章
 *
 * CN 章节正文+元数据——故事主体的最小单元
 * JP 章の本文+メタデータ——物語の最小単位
 * EN Chapter content + metadata — the smallest unit of story
 */

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chapters", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "chapterNumber"})
}, indexes = {
        @Index(name = "idx_chapters_phase", columnList = "phase"),
        @Index(name = "idx_chapters_number", columnList = "chapter_number")
})
@Getter
@Setter
public class Chapter {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @Column(columnDefinition = "text")
    private String phase;

    @Column
    private Integer wordCount;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(columnDefinition = "text")
    private String filePath;

    @Column(columnDefinition = "text")
    private String fileHash;

    @Column(length = 20)
    private String status;

    @Column(updatable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;
}
