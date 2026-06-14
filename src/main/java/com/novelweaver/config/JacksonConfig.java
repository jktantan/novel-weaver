package com.novelweaver.config;

/*
 * Jackson Configuration / Jackson 配置 / Jackson 設定
 *
 * CN 共享 ObjectMapper Bean，统一 JSON 序列化行为
 * JP 共有 ObjectMapper Bean、JSON シリアライズを統一
 * EN Shared ObjectMapper Bean for consistent JSON serialization
 */

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(SerializationFeature.INDENT_OUTPUT, false);
    }
}
