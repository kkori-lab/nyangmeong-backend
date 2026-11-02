package com.nyangmeong.global.external;

import java.time.LocalDate;

import lombok.Data;

/**
 * [외부 API 사용량 검색 조건]
 * - 조건은 모두 선택, 기간이 없으면 service 가 최근 7일로 채움
 */
@Data
public class ExternalApiUsageSearchCond {
    /** 시작일자 */
    private LocalDate bgngYmd;
    /** 종료일자 */
    private LocalDate endYmd;
    /** 외부API구분공통코드 */
    private String extlApiSeCd;
}
