package com.readcamp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.readcamp.entity.Chapter;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/** 章节 Mapper（重切分/删文章级联用） */
public interface ChapterMapper extends BaseMapper<Chapter> {

    /** 按文章删除全部章节（级联：重切分/删文章） */
    @Delete("DELETE FROM chapter WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Long articleId);
}
