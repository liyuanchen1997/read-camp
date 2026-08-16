package com.readcamp.service.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SentenceSplitterTest {

    @Test
    void splitBasicSentences() {
        List<String> r = SentenceSplitter.split(
                "The cat sat on the mat. The dog barked loudly! Is it raining?");
        assertEquals(List.of(
                "The cat sat on the mat.",
                "The dog barked loudly!",
                "Is it raining?"), r);
    }

    @Test
    void abbreviationMrNotSplit() {
        List<String> r = SentenceSplitter.split("Mr. Smith is here. He is a teacher.");
        assertEquals(2, r.size());
        assertTrue(r.get(0).startsWith("Mr. Smith"));
    }

    @Test
    void decimalNumberNotSplit() {
        List<String> r = SentenceSplitter.split("Pi is 3.14 approximately. It is irrational.");
        assertEquals(2, r.size());
        assertTrue(r.get(0).contains("3.14"));
    }

    @Test
    void multiPartAbbreviationNotSplit() {
        List<String> r = SentenceSplitter.split("The U.S. economy is growing. So is the U.K.'s.");
        assertEquals(2, r.size());
        assertTrue(r.get(0).startsWith("The U.S. economy"));
    }

    @Test
    void egAbbreviationNotSplit() {
        List<String> r = SentenceSplitter.split("Use tools, e.g. a hammer. That is all.");
        assertEquals(2, r.size());
        assertTrue(r.get(0).contains("e.g. a hammer"));
    }

    @Test
    void quotedSentenceKeepsQuote() {
        List<String> r = SentenceSplitter.split("\"Hello world.\" The reply came.");
        assertEquals(2, r.size());
        assertEquals("\"Hello world.\"", r.get(0));
    }

    @Test
    void midQuotePeriodNotSplit() {
        List<String> r = SentenceSplitter.split(
                "\"Please let me go,\" cried the mouse. The lion laughed.");
        assertEquals(2, r.size());
        assertTrue(r.get(0).endsWith("cried the mouse."));
    }

    @Test
    void newlinesAndSpacesNormalized() {
        List<String> r = SentenceSplitter.split("First sentence.\n\nSecond sentence.\n\nThird.");
        assertEquals(List.of("First sentence.", "Second sentence.", "Third."), r);
    }

    @Test
    void blankAndNullInput() {
        assertTrue(SentenceSplitter.split(null).isEmpty());
        assertTrue(SentenceSplitter.split("   \n  ").isEmpty());
    }

    @Test
    void noTerminalPunctuation() {
        List<String> r = SentenceSplitter.split("A sentence without punctuation");
        assertEquals(List.of("A sentence without punctuation"), r);
    }
}
