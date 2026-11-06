package com.nyangmeong.ai.domain;

import lombok.Data;

/**
 * [키워드 사전 VO]
 * - KWRD_DICT : 대화창 일정 등록 판정(AIMC0037)·가계부 분류(AIMC0038) 키워드 (FN-ADM-081)
 * - 뉴스 용도(AIMC0039·0040) 행은 community가 다룬다 (SCR-ADM-050)
 */
@Data
public class KwrdDict {

    /** 키워드사전일련번호 */
    private Long kwrdDictSn;
    /** 사전용도공통코드 (AIMG10) */
    private String dictUsgCd;
    /** 키워드분류명 */
    private String kwrdClsfNm;
    /** 항목명 */
    private String artclNm;
    /** 키워드내용 (쉼표 구분 예시 문구) */
    private String kwrdCn;
    /** 해석결과내용 */
    private String intrpRsltCn;
    /** 매핑공통코드 */
    private String mpngCd;
    /** 매핑공통코드명 (COM_CD 조인, 조회 전용) */
    private String mpngCdNm;
    /** 정렬순서 */
    private Integer sortSeq;
    /** 비고내용 */
    private String rmrkCn;
    /** 사용여부 */
    private String useYn;
}
