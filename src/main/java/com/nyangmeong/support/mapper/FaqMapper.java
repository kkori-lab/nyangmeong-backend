package com.nyangmeong.support.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.support.dto.FaqSummary;

/**
 * [자주 묻는 질문 Mapper]
 */
@Mapper
public interface FaqMapper {

    /**
     * FAQ 목록 조회 (카테고리·질문·답변 검색, 정렬 순서)
     */
    List<FaqSummary> selectList(@Param("faqClsfCd") String faqClsfCd, @Param("keyword") String keyword);
}
