package com.novelweaver.service;

/*
 * ChapterService Unit Tests / 章节服务单元测试
 * Tests pure text-processing methods: textTruncate(), segment(), classify().
 * All are static methods with no dependency on injected services.
 */

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChapterServiceTest {

    // ── textTruncate ──

    @Test
    void textTruncate_shortText_returnsAsIs() {
        assertEquals("hello", ChapterService.textTruncate("hello", 100));
    }

    @Test
    void textTruncate_longText_truncatesWithEllipsis() {
        String result = ChapterService.textTruncate("hello world", 5);
        assertTrue(result.startsWith("hello"));
        assertTrue(result.endsWith("…"));
    }

    @Test
    void textTruncate_null_returnsEmpty() {
        assertEquals("", ChapterService.textTruncate(null, 100));
    }

    @Test
    void textTruncate_exactLength_returnsAsIs() {
        String input = "abcde";
        String result = ChapterService.textTruncate(input, 5);
        assertEquals(input, result);
        assertFalse(result.endsWith("…"));
    }

    @Test
    void textTruncate_emptyString_returnsEmpty() {
        assertEquals("", ChapterService.textTruncate("", 10));
    }

    @Test
    void textTruncate_zeroMaxPoints_returnsEllipsisOnly() {
        String result = ChapterService.textTruncate("hello", 0);
        assertEquals("…", result);
    }

    // ── segment ──

    @Test
    void segment_empty_returnsSingleEmptySegment() {
        // split on empty string produces [""] — one empty block
        List<ChapterService.Segment> segs = ChapterService.segment("");
        assertEquals(1, segs.size());
        assertEquals("", segs.get(0).scene());
        assertEquals("", segs.get(0).text());
    }

    @Test
    void segment_blank_returnsEmpty() {
        List<ChapterService.Segment> segs = ChapterService.segment("   \n\n  ");
        // Whitespace-only content — headline will be empty/whitespace, body empty
        assertFalse(segs.isEmpty());
        assertEquals("", segs.get(0).scene().trim());
    }

    @Test
    void segment_singleScene_returnsOneSegment() {
        List<ChapterService.Segment> segs = ChapterService.segment("## 第一章\n这是第一章的内容。");
        assertEquals(1, segs.size());
        assertEquals("第一章", segs.get(0).scene());
        assertTrue(segs.get(0).text().contains("## 第一章"));
    }

    @Test
    void segment_multipleScenes_returnsMultipleSegments() {
        List<ChapterService.Segment> segs = ChapterService.segment(
                "## 场景一\n场景一的内容。\n## 场景二\n场景二的内容。");
        assertEquals(2, segs.size());
        assertEquals("场景一", segs.get(0).scene());
        assertEquals("场景二", segs.get(1).scene());
    }

    @Test
    void segment_headlineStripsHashMarks() {
        List<ChapterService.Segment> segs = ChapterService.segment("### 三级标题\n内容");
        assertEquals(1, segs.size());
        assertEquals("三级标题", segs.get(0).scene());
    }

    @Test
    void segment_noHeading_wholeTextBecomesHeadline() {
        // Without ## markers, entire text treated as one block;
        // headline extraction strips # prefix — none here, so full text is headline
        List<ChapterService.Segment> segs = ChapterService.segment("这是一段没有标题的纯文本。");
        assertEquals(1, segs.size());
        assertEquals("这是一段没有标题的纯文本。", segs.get(0).scene());
    }

    // ── classify ──

    @Test
    void classify_dialogue_highQuoteRatio() {
        // 6 quotes in ~50 chars > 5% ratio
        String result = ChapterService.classify(
                "\"你好\"他说。\"好久不见\"她说。\"是啊\"他答。");
        assertEquals("dialogue", result);
    }

    @Test
    void classify_narrative_lowQuoteRatio() {
        String result = ChapterService.classify(
                "他独自走在街上。天气很好。路边花开得正艳。");
        assertEquals("narrative", result);
    }

    @Test
    void classify_emptyText_returnsNarrative() {
        assertEquals("narrative", ChapterService.classify(""));
    }

    @Test
    void classify_mostlyNarrative_lowQuoteRatio() {
        String result = ChapterService.classify(
                "He said \"wait\" and walked on. The sun was warm and the breeze " +
                        "gently stirred the leaves. Nobody paid him any attention as he " +
                        "continued down the quiet street, hands in his pockets.");
        assertEquals("narrative", result);
    }

    // ── segment + long content splitting ──

    @Test
    void segment_longContent_splitsIntoChunks() {
        // Build >800 codepoint text (no ## headings to force chunking)
        StringBuilder sb = new StringBuilder();
        sb.append("## 长场景\n");
        for (int i = 0; i < 100; i++) {
            sb.append("这是第").append(i).append("段测试文本。为了凑够长度需要多写一些字。");
        }
        List<ChapterService.Segment> segs = ChapterService.segment(sb.toString());
        assertTrue(segs.size() > 1, "Long content should be split into chunks, got " + segs.size());
        // Check chunk names have sequence numbers
        assertTrue(segs.get(0).scene().contains("长场景"));
        assertTrue(segs.get(1).scene().contains("长场景") && segs.get(1).scene().contains("[2]"));
    }

    @Test
    void segment_unicodeContent_preservesCharacters() {
        String text = "## 测试\n包含emoji🌍和中文：你好世界✨。";
        List<ChapterService.Segment> segs = ChapterService.segment(text);
        assertEquals(1, segs.size());
        assertTrue(segs.get(0).text().contains("🌍"));
        assertTrue(segs.get(0).text().contains("✨"));
    }

    // ── segment + Windows line endings ──

    @Test
    void segment_windowsLineEndings_splitsCorrectly() {
        String text = "## 场景A\r\n内容A\r\n## 场景B\r\n内容B";
        List<ChapterService.Segment> segs = ChapterService.segment(text);
        assertEquals(2, segs.size());
        assertEquals("场景A", segs.get(0).scene());
        assertEquals("场景B", segs.get(1).scene());
    }

    @Test
    void segment_mixedLineEndings_handlesGracefully() {
        String text = "## 场景A\n内容A\r\n## 场景B\r\n内容B";
        List<ChapterService.Segment> segs = ChapterService.segment(text);
        assertEquals(2, segs.size());
    }
}
