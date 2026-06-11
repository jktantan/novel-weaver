package com.novelweaver.config;

/*
 * Health Check / 健康检查 / ヘルスチェック
 *
 * CN Docker 健康检查端点
 * JP Docker ヘルスチェック用エンドポイント
 * EN Docker health check endpoint
 */

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Docker compose depends_on 健康检查。
 * MCP 端点 /mcp 由框架自动提供，这个只做存活检测。
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "ok";
    }
}
