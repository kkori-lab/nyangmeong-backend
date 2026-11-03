package com.nyangmeong.community.dto;

import lombok.Data;

/**
 * [관리자 실종제보 검색 조건]
 * - 조건은 모두 선택
 */
@Data
public class AdminLostReportSearchCond {
    /** 실종제보상태공통코드 (만료는 기간 지난 실종 포함) */
    private String lostTpofSttsCd;
}
