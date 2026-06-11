package com.novelweaver.model;

/*
 * Chapter Version / 章节版本 / 章バージョン
 *
 * CN 章节版本历史——每次 chapter_sync 自动生成新版本
 * JP 章のバージョン履歴——chapter_sync のたびに新バージョン
 * EN Chapter version history — auto-generated on each sync
 */

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chapter_versions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"chapter_id", "version"})
})
@Getter
@Setter
public class ChapterVersion {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;
    @Column(nullable = false)
    private Integer version;
    @Column(columnDefinition = "text")
    private String content;
    @Column
    private Integer wordCount;
    @Column(columnDefinition = "text")
    private String fileHash;
    @Column(updatable = false)
    private Instant createdAt;
}
