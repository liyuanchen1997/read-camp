package com.readcamp.dto;

import com.readcamp.entity.UserVocab;
import lombok.Data;

import java.time.LocalDateTime;

/** 生词本列表项 */
@Data
public class VocabItem {

    private Long id;
    private String word;
    private Long sourceArticleId;
    /** 出处原句 */
    private String contextSentence;
    /** 词性（来自出处句 AI 标注，可能为 null） */
    private String pos;
    /** 中文意思（来自出处句 AI 标注，可能为 null） */
    private String meaning;
    /** 在句中的作用（来自出处句 AI 标注，可能为 null） */
    private String role;
    /** 英语音标 IPA（来自出处句 AI 标注，可能为 null） */
    private String phonetic;
    private LocalDateTime createdAt;

    public static VocabItem from(UserVocab vocab) {
        VocabItem item = new VocabItem();
        item.setId(vocab.getId());
        item.setWord(vocab.getWord());
        item.setSourceArticleId(vocab.getSourceArticleId());
        item.setContextSentence(vocab.getContextSentence());
        item.setCreatedAt(vocab.getCreatedAt());
        return item;
    }
}
