package com.readcamp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.readcamp.entity.UserFavoriteSentence;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface UserFavoriteSentenceMapper extends BaseMapper<UserFavoriteSentence> {

    /** 删除某文章下所有句子的收藏（级联用） */
    @Delete("DELETE FROM user_favorite_sentence WHERE sentence_id IN "
            + "(SELECT id FROM sentence WHERE article_id = #{articleId})")
    int deleteByArticleId(@Param("articleId") Long articleId);
}
