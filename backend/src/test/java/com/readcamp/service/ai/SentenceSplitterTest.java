package com.readcamp.service.ai;

import com.readcamp.service.ai.SentenceSplitter.SentencePart;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SentenceSplitterTest {

    private static List<String> texts(String input) {
        return SentenceSplitter.split(input).stream().map(SentencePart::text).toList();
    }

    @Test
    void splitBasicSentences() {
        List<String> r = texts(
                "The cat sat on the mat. The dog barked loudly! Is it raining?");
        assertEquals(List.of(
                "The cat sat on the mat.",
                "The dog barked loudly!",
                "Is it raining?"), r);
    }

    @Test
    void abbreviationMrNotSplit() {
        List<String> r = texts("Mr. Smith is here. He is a teacher.");
        assertEquals(2, r.size());
        assertTrue(r.get(0).startsWith("Mr. Smith"));
    }

    @Test
    void decimalNumberNotSplit() {
        List<String> r = texts("Pi is 3.14 approximately. It is irrational.");
        assertEquals(2, r.size());
        assertTrue(r.get(0).contains("3.14"));
    }

    @Test
    void multiPartAbbreviationNotSplit() {
        List<String> r = texts("The U.S. economy is growing. So is the U.K.'s.");
        assertEquals(2, r.size());
        assertTrue(r.get(0).startsWith("The U.S. economy"));
    }

    @Test
    void egAbbreviationNotSplit() {
        List<String> r = texts("Use tools, e.g. a hammer. That is all.");
        assertEquals(2, r.size());
        assertTrue(r.get(0).contains("e.g. a hammer"));
    }

    @Test
    void quotedSentenceKeepsQuote() {
        List<String> r = texts("\"Hello world.\" The reply came.");
        assertEquals(2, r.size());
        assertEquals("\"Hello world.\"", r.get(0));
    }

    @Test
    void midQuotePeriodNotSplit() {
        List<String> r = texts(
                "\"Please let me go,\" cried the mouse. The lion laughed.");
        assertEquals(2, r.size());
        assertTrue(r.get(0).endsWith("cried the mouse."));
    }

    @Test
    void newlinesNormalizedWithinParagraph() {
        // 存在空行（空行分段风格）：段内单个换行折叠为空格，不产生新段落
        List<SentencePart> parts = SentenceSplitter.split(
                "First sentence.\nStill first paragraph.\nSecond sentence.\n\nNext paragraph.");
        assertEquals(4, parts.size());
        assertEquals("Still first paragraph.", parts.get(1).text());
        assertEquals(0, parts.get(1).para());
        assertEquals("Next paragraph.", parts.get(3).text());
        assertEquals(1, parts.get(3).para());
    }

    @Test
    void singleNewlineSplitsParagraph() {
        // 全文无空行（单换行分段风格，用户粘贴常见）：单个换行即段落边界
        List<SentencePart> parts = SentenceSplitter.split(
                "Billy is ten. He is big.\nThis is Tom. He is small.\nThey have bunk-beds.");
        assertEquals(5, parts.size());
        assertEquals(0, parts.get(0).para());
        assertEquals(0, parts.get(1).para());
        assertEquals(1, parts.get(2).para());
        assertEquals(1, parts.get(3).para());
        assertEquals(2, parts.get(4).para());
    }

    @Test
    void gluedSentencesWithoutSpaceSplit() {
        // 句子粘连无空格：点后紧跟大写字母，点前是完整单词（≥3 字符）→ 仍视为句界
        List<String> r = texts("He is not strong.Tom is not happy.");
        assertEquals(List.of("He is not strong.", "Tom is not happy."), r);
    }

    @Test
    void unknownMultiDotAbbreviationKept() {
        // 未知缩写（A.B.C.）：内部点（A.、B.）点前仅 1 字符 → 不切；
        // 结尾点 C. 后跟空格按常规句界切分（原行为，缩写结束处不特判）
        List<String> r = texts("The A.B.C. company grew. It doubled.");
        assertEquals(3, r.size());
        assertTrue(r.get(0).contains("A.B"));
    }

    @Test
    void trailingNewlineKeepsConsecutiveParaIds() {
        // 换行开头/结尾的空白段跳过，段号保持 0,1,2… 连续
        List<SentencePart> parts = SentenceSplitter.split("\nPara one.\n\n\nPara two.\n");
        assertEquals(2, parts.size());
        assertEquals(0, parts.get(0).para());
        assertEquals(1, parts.get(1).para());
    }

    @Test
    void blankLineSplitsParagraph() {
        List<SentencePart> parts = SentenceSplitter.split(
                "First paragraph sentence one. First paragraph sentence two.\n\nSecond paragraph sentence.");
        assertEquals(3, parts.size());
        assertEquals(0, parts.get(0).para());
        assertEquals(0, parts.get(1).para());
        assertEquals(1, parts.get(2).para());
    }

    @Test
    void crlfParagraphSplit() {
        List<SentencePart> parts = SentenceSplitter.split("Para one.\r\n\r\nPara two.");
        assertEquals(2, parts.size());
        assertEquals(0, parts.get(0).para());
        assertEquals(1, parts.get(1).para());
    }

    @Test
    void blankAndNullInput() {
        assertTrue(SentenceSplitter.split(null).isEmpty());
        assertTrue(SentenceSplitter.split("   \n  ").isEmpty());
    }

    @Test
    void noTerminalPunctuation() {
        List<String> r = texts("A sentence without punctuation");
        assertEquals(List.of("A sentence without punctuation"), r);
    }
}
