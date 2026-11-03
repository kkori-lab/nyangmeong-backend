package com.nyangmeong.support.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.support.domain.Faq;
import com.nyangmeong.support.dto.AdminFaqSummary;
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

    /**
     * 관리자 FAQ 목록 조회 (비노출 포함, 카테고리·질문·답변 검색) - SCR-ADM-041
     */
    List<AdminFaqSummary> selectAdminList(@Param("faqClsfCd") String faqClsfCd, @Param("keyword") String keyword);

    /**
     * 관리자 FAQ 단건 조회 (사용 여부 무관)
     */
    Faq selectAdminFaq(@Param("faqSn") Long faqSn);

    /**
     * FAQ 등록 (faqSn 채움)
     */
    int insert(Faq faq);

    /**
     * FAQ 수정 (null 인 값은 그대로)
     */
    int update(Faq faq);
}
