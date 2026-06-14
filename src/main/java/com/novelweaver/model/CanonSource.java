package com.novelweaver.model;

/*
 * Canon Source / 正典来源 / 正典ソース
 *
 * CN 正典资料来源——仅同人项目
 * JP 正典資料ソース——二次創作のみ
 * EN Canon source record — used only for fanfic
 */

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "canon_sources", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "name"})
})
@Getter
@Setter
public class CanonSource {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    @Column(nullable = false)
    private String name;
    @Column(columnDefinition = "text")
    private String url;
    @Column(columnDefinition = "text")
    private String content;
    @Column
    private Boolean verified;
    @Column(updatable = false)
    private Instant createdAt;
}
