package com.nyangmeong.community.dto;

import lombok.Data;

/**
 * [실종 제보 검색 조건]
 * - 모두 선택, 주면 AND (FN-CMU-002-02·03·04)
 * - wtnsPrd : TODAY(오늘), WEEK(1주 이내), MONTH(1개월 이내), OVER_MONTH(1개월 이상), 최종 목격 일시 기준
 * - rgnSn 은 그 지역과 하위 지역 전체
 */
@Data
public class LostReportSearchCond {
    private String lostTpofSttsCd;
    private Long rgnSn;
    private String petSeCd;
    private String wtnsPrd;
    private String keyword;
}
