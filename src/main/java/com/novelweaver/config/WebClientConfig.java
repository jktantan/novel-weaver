package com.novelweaver.config;

/*
 * HTTP Client Config / HTTP 客户端配置 / HTTPクライアント設定
 *
 * CN Meilisearch + LanguageTool HTTP 客户端
 * JP Meilisearch + LanguageTool の HTTP クライアント
 * EN HTTP client for Meilisearch + LanguageTool
 */

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Meilisearch、LanguageTool 等外部 HTTP 服务共用的 WebClient。
 * 各 Service 注入 {@code WebClient.Builder} 后自行设置 baseUrl 和 header。
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
