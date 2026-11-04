package com.nyangmeong.community.domain;

import lombok.Data;

/**
 * [뉴스 분류 키워드]
 * - KWRD_DICT 중 뉴스 분류 용도(AIMC0039)의 행
 * - kwrdCn : 키워드. 관리자 화면(SCR-ADM-050)은 한 행에 하나, 지난 기준 데이터는 쉼표로 이어진 행일 수 있다
 * - kwrdDictSn·sortSeq 는 관리자 키워드 관리(FN-ADM-050-05)에서만 채운다
 */
@Data
public class NewsKeyword {
    private Long kwrdDictSn;
    private String mpngCd;
    private String kwrdCn;
    private Integer sortSeq;
}
