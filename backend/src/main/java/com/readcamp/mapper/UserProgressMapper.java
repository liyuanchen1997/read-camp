package com.readcamp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.readcamp.entity.UserProgress;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface UserProgressMapper extends BaseMapper<UserProgress> {

    /** 删除某文章的全部进度（编辑重切分/删除文章级联用） */
    @Delete("DELETE FROM user_progress WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Long articleId);
}
