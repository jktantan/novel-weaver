package com.novelweaver.model;

/*
 * Universe / 宇宙 / 宇宙
 *
 * CN 宇宙容器——一个项目可有多个宇宙
 * JP 宇宙コンテナ——1プロジェクトで複数の宇宙を持てる
 * EN Universe container — a project can have multiple universes
 */

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "universes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "name"})
}, indexes = {
        @Index(name = "idx_universes_project", columnList = "project_id")
})
@Getter
@Setter
public class Universe {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(columnDefinition = "text")
    private String description;

    @Column(updatable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;
}
