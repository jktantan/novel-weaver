package com.novelweaver.service;

/*
 * Grammar Service / 语法检查 / 文法チェック
 *
 * CN 调用 LanguageTool 做语法检查
 * JP LanguageTool を呼び出して文法チェック
 * EN Call LanguageTool for grammar checking
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GrammarService {

    private static final Logger log = LoggerFactory.getLogger(GrammarService.class);

    private final WebClient ltClient;

    public GrammarService(WebClient.Builder wcb,
                          @Value("${novel.languagetool.url}") String ltUrl) {
        this.ltClient = wcb.baseUrl(ltUrl).build();
    }

    /*
     * 语法检查 / 文法チェック / Check
     *
     * CN 检查中文文本的语法、错别字、标点符号
     * JP 中国語テキストの文法・誤字・句読点をチェック
     * EN Check Chinese text for grammar, typos, punctuation
     */
    @McpTool(name = "grammar_check", description = "语法检查——调用 LanguageTool 检查中文文本的语法、错别字、标点，返回错误列表和修改建议 | CN 语法检查 / JP 文法チェック / EN Grammar check")
    public GrammarCheckResult check(
            @McpToolParam(description = "要检查的文本", required = true) String text,
            @McpToolParam(description = "语言代码（默认 zh-CN）", required = false) String language) {

        String lang = language != null ? language : "zh-CN";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", text);
        body.put("language", lang);

        try {
            Map<?, ?> resp = ltClient.post()
                    .uri("/check")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (resp == null) {
                return new GrammarCheckResult("error", 0, List.of(), "LanguageTool returned null");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matches;
            Object raw = resp.get("matches");
            if (raw instanceof List<?> list) {
                matches = (List<Map<String, Object>>) list;
            } else {
                matches = List.of();
            }

            List<GrammarMatch> result = new ArrayList<>();
            for (Map<String, Object> m : matches) {
                String message = (String) m.getOrDefault("message", "");
                String shortMsg = (String) m.getOrDefault("shortMessage", "");
                int offset = m.get("offset") instanceof Number ? ((Number) m.get("offset")).intValue() : 0;
                int length = m.get("length") instanceof Number ? ((Number) m.get("length")).intValue() : 0;

                // 上下文：出错位置前后 40 字符
                int ctxStart = Math.max(0, offset - 20);
                int ctxEnd = Math.min(text.length(), offset + length + 20);
                String context = text.substring(ctxStart, ctxEnd);

                List<String> replacements = new ArrayList<>();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> reps = (List<Map<String, Object>>) m.getOrDefault("replacements", List.of());
                for (Map<String, Object> r : reps) {
                    Object val = r.get("value");
                    if (val instanceof String s) replacements.add(s);
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> rule = (Map<String, Object>) m.get("rule");
                String ruleId = rule != null ? (String) rule.getOrDefault("id", "") : "";
                String ruleDesc = rule != null ? (String) rule.getOrDefault("description", "") : "";

                result.add(new GrammarMatch(message, shortMsg, offset, length, context, replacements, ruleId, ruleDesc));
            }

            return new GrammarCheckResult("ok", result.size(), result,
                    result.isEmpty() ? "未发现语法问题" : null);

        } catch (Exception e) {
            log.warn("LanguageTool check failed (url={}, lang={}): {}", ltClient, lang, e.getMessage());
            return new GrammarCheckResult("error", 0, List.of(),
                    "LanguageTool 不可用: " + e.getMessage());
        }
    }

    // ── result records ──

    public record GrammarCheckResult(String status, int count, List<GrammarMatch> matches, String note) {
    }

    public record GrammarMatch(String message, String shortMessage, int offset, int length,
                               String context, List<String> replacements, String ruleId, String ruleDescription) {
    }
}
