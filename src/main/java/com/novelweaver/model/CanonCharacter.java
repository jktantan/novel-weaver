package com.novelweaver.model;

/*
 * Canon Character / 正典人物 / 正典キャラクター
 *
 * CN 正典人物——从原作提取的不可变更事实
 * JP 正典キャラクター——原作から抽出した不変情報
 * EN Canon characters — immutable facts from source
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
@Table(name = "canon_characters", indexes = @Index(name = "idx_canon_char_project", columnList = "project_id"))
@Getter
@Setter
public class CanonCharacter {
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
    private String name;
    @Column(columnDefinition = "text[]")
    private String[] aliases;
    @Column(columnDefinition = "text")
    private String bio;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String traits;
    @Column(columnDefinition = "vector(1024)")
    @Type(PgVectorType.class)
    private String embedding;
    @Column
    private Boolean verified;
    @Column(updatable = false)
    private Instant createdAt;
}
