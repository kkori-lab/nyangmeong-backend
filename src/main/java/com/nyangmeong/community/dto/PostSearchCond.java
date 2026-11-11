package com.nyangmeong.community.dto;

import com.nyangmeong.global.aitool.AiCode;

import lombok.Data;

/**
 * [게시글 검색 조건]
 * - 게시판 필터와 키워드, 없으면 전체 (FN-CMU-001-02·08)
 * - mineYn : true 면 내 글만
 */
@Data
public class PostSearchCond {
    @AiCode("CMUG01")
    private String bbsCd;
    private String keyword;
    /** true 면 로그인 회원의 글만 (생략하면 전체) */
    private boolean mineYn;
}
