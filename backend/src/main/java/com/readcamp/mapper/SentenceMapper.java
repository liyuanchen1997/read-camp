package com.readcamp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.readcamp.entity.Sentence;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface SentenceMapper extends BaseMapper<Sentence> {

    /** 删除文章的全部句子（级联用，返回删除行数） */
    @Delete("DELETE FROM sentence WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Long articleId);
}
