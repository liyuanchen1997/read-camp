package com.readcamp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.readcamp.entity.SentenceAnnotation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface SentenceAnnotationMapper extends BaseMapper<SentenceAnnotation> {

    /** 删除一批句子的标注（按句子 id 集合，级联用） */
    @Delete("<script>"
            + "DELETE FROM sentence_annotation WHERE sentence_id IN "
            + "<foreach collection='sentenceIds' item='sid' open='(' separator=',' close=')'>#{sid}</foreach>"
            + "</script>")
    int deleteBySentenceIds(@Param("sentenceIds") java.util.Collection<Long> sentenceIds);
}
