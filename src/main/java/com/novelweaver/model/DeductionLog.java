package com.novelweaver.model;

/*
 * Deduction Log / 推演日志 / 推論ログ
 *
 * CN 推演操作历史记录
 * JP 推論操作の履歴記録
 * EN Deduction operation history log
 */

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deduction_logs", indexes = {
        @Index(name = "idx_deduction_project", columnList = "project_id"),
        @Index(name = "idx_deduction_chapter", columnList = "chapter_id")
})
@Getter
@Setter
public class DeductionLog {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;
    @Column(nullable = false, length = 30)
    private String type;
    @Column
    private String model;
    @Column(columnDefinition = "text")
    private String inputContext;
    @Column(columnDefinition = "text")
    private String outputResult;
    @Column
    private Integer tokensIn;
    @Column
    private Integer tokensOut;
    @Column(precision = 10, scale = 6)
    private BigDecimal cost;
    @Column(updatable = false)
    private Instant createdAt;
}
