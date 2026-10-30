package com.nyangmeong.support.dto;

import lombok.Data;

/**
 * [FAQ 검색 조건]
 * - faqClsfCd : 카테고리 코드(CSCG01), 없거나 공백이면 전체 (FN-CSC-002-02)
 * - keyword : 질문·답변 검색, 없거나 공백이면 전체, 카테고리와 AND (FN-CSC-002-03)
 */
@Data
public class FaqSearchCond {
    private String faqClsfCd;
    private String keyword;
}
