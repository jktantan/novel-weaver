package com.novelweaver.model;

/*
 * Project / 项目 / プロジェクト
 *
 * CN 项目元信息——每个项目包含一部小说的全部数据
 * JP プロジェクトメタ情報——各プロジェクトは1つの小説の全データを含む
 * EN Project metadata — each project contains all data for one novel
 */

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter
@Setter
public class Project {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @Column(nullable = false, length = 10)
    private String type;

    @Column(length = 20)
    private String status;

    @Column(columnDefinition = "text")
    private String vaultPath;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String meta;

    @Column(updatable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;
}
