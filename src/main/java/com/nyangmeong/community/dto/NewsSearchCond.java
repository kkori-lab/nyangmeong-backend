package com.nyangmeong.community.dto;

import lombok.Data;

/**
 * [뉴스 검색 조건]
 * - newsClsfCd : 카테고리 코드(CMUG06), 없거나 공백이면 전체 (FN-CMU-003-02)
 * - keyword : 제목·내용 검색, 없거나 공백이면 전체, 카테고리와 AND (FN-CMU-003-03)
 */
@Data
public class NewsSearchCond {
    private String newsClsfCd;
    private String keyword;
}
