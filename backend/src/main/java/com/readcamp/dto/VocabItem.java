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
