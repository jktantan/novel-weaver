package com.novelweaver.config;

/*
 * Health Check / 健康检查 / ヘルスチェック
 *
 * CN Docker 健康检查端点 — 返回 PG + Neo4j 连通性
 * JP Docker ヘルスチェック用エンドポイント — PG + Neo4j 疎通確認
 * EN Docker health check endpoint — returns PG + Neo4j connectivity
 */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private Neo4jClient neo4j;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();

        // PG 连通性检查
        boolean pgOk = false;
        if (dataSource != null) {
            try (Connection c = dataSource.getConnection()) {
                pgOk = c.isValid(2);
            } catch (Exception ignored) {
            }
        }
        status.put("postgresql", pgOk ? "UP" : "DOWN");

        // Neo4j 连通性检查
        boolean neoOk = false;
        if (neo4j != null) {
            try {
                neo4j.query("RETURN 1").fetch().all();
                neoOk = true;
            } catch (Exception ignored) {
            }
        }
        status.put("neo4j", neoOk ? "UP" : "DOWN");

        // 整体状态
        boolean allUp = pgOk && neoOk;
        status.put("status", allUp ? "UP" : "DEGRADED");

        return ResponseEntity.ok(status);
    }
}
