package com.novelweaver.model;

/*
 * Character Relationship / 人物关系 / 人物関係
 *
 * CN 人物关系表
 * JP 人物関係テーブル
 * EN Character relationships table
 */

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "character_relationships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "fromChar", "toChar", "relationType"})
}, indexes = {
        @Index(name = "idx_relationships_from", columnList = "from_char"),
        @Index(name = "idx_relationships_to", columnList = "to_char"),
        @Index(name = "idx_relationships_project", columnList = "project_id")
})
@Getter
@Setter
public class CharacterRelationship {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    @Column(nullable = false)
    private String fromChar;
    @Column(nullable = false)
    private String toChar;
    @Column(nullable = false)
    private String relationType;
    @Column(columnDefinition = "text")
    private String trustLevel;
    @Column(columnDefinition = "text")
    private String note;
    @Column(columnDefinition = "text")
    private String sourceFile;
    @Column(updatable = false)
    private Instant createdAt;
    @Column
    private Instant updatedAt;
}
