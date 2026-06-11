package com.novelweaver.model;

/*
 * Canon Relationship / 正典关系 / 正典関係
 *
 * CN 正典人物关系——不可变更的原作关系
 * JP 正典人物関係——変更不可の原作関係
 * EN Canon relationships — immutable original relationships
 */

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "canon_relationships", indexes = @Index(name = "idx_canon_rel_project", columnList = "project_id"))
@Getter
@Setter
public class CanonRelationship {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private CanonSource source;
    @Column(nullable = false)
    private String fromChar;
    @Column(nullable = false)
    private String toChar;
    @Column(nullable = false)
    private String relType;
    @Column(columnDefinition = "text")
    private String description;
    @Column
    private Boolean verified;
    @Column(updatable = false)
    private Instant createdAt;
}
