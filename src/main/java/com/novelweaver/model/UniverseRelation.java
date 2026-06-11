package com.novelweaver.model;

/*
 * Universe Relation / 宇宙关系 / 宇宙間関係
 *
 * CN 宇宙之间的关系——平行/衍生/跨界
 * JP 宇宙間の関係——並行/派生/クロス
 * EN Universe relations — parallel/derived/crossover
 */

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "universe_relations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "from_universe_id", "to_universe_id"})
}, indexes = {
        @Index(name = "idx_univ_rel_project", columnList = "project_id"),
        @Index(name = "idx_univ_rel_from", columnList = "from_universe_id"),
        @Index(name = "idx_univ_rel_to", columnList = "to_universe_id")
})
@Getter
@Setter
public class UniverseRelation {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_universe_id", nullable = false)
    private Universe fromUniverse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_universe_id", nullable = false)
    private Universe toUniverse;

    @Column(nullable = false)
    private String relationType;

    @Column(columnDefinition = "text")
    private String description;

    @Column(updatable = false)
    private Instant createdAt;
}
