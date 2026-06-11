package com.novelweaver.model;

/*
 * Foreshadowing / 伏笔 / 伏線
 *
 * CN 伏笔登记——追踪伏笔的埋设和回收
 * JP 伏線登録——伏線の設置と回収を追跡
 * EN Foreshadowing register — track setup and payoff
 */

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "foreshadowing_index", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "code"})
}, indexes = @Index(name = "idx_foreshadowing_project", columnList = "project_id"))
@Getter
@Setter
public class Foreshadowing {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    @Column(nullable = false)
    private String code;
    @Column(columnDefinition = "text")
    private String description;
    @Column
    private String fType;
    @Column
    private Integer plantedChapter;
    @Column
    private Integer payoffChapter;
    @Column
    private String status;
    @Column(columnDefinition = "text[]")
    private String[] relatedCharacters;
    @Column(updatable = false)
    private Instant createdAt;
    @Column
    private Instant updatedAt;
}
