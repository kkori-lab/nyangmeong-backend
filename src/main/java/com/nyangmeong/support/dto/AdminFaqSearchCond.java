package com.nyangmeong.support.dto;

import lombok.Data;

/**
 * [관리자 FAQ 검색 조건]
 * - faqClsfCd : 공통코드 CSCG01, 없거나 공백이면 전체 (FN-ADM-041-01·04)
 * - keyword : 질문·답변 부분 일치, 카테고리와 AND (FN-ADM-041-01)
 */
@Data
public class AdminFaqSearchCond {
    private String faqClsfCd;
    private String keyword;
}
