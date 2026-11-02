package com.nyangmeong.community.domain;

import lombok.Data;

/**
 * [뉴스 분류 키워드]
 * - KWRD_DICT 중 뉴스 분류 용도(AIMC0039)의 분류 코드와 키워드 목록만
 * - kwrdCn : 쉼표로 구분한 키워드
 */
@Data
public class NewsKeyword {
    private String mpngCd;
    private String kwrdCn;
}
