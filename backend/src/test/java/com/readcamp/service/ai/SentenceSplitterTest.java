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
        // 段内单个换行折叠为空格：句子正常切分，且不产生新段落
        List<SentencePart> parts = SentenceSplitter.split(
                "First sentence.\nStill first paragraph.\nSecond sentence.");
        assertEquals(3, parts.size());
        assertEquals("Still first paragraph.", parts.get(1).text());
        assertEquals(0, parts.get(1).para());
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
