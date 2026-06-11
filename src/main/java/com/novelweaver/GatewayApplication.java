package com.novelweaver;

/*
 * Novel Weaver Gateway
 *
 * CN Spring Boot MCP Gateway 入口——小说写作辅助系统的数据后端
 * JP Spring Boot MCP Gateway エントリーポイント——小説執筆支援システムのデータバックエンド
 * EN Spring Boot MCP Gateway entry point — data backend for novel writing assistant
 */

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
