package com.nyangmeong.global.external;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

/**
 * [관리자 검사결과지 분석 현황 검색 조건]
 * - SCR-ADM-082 (FN-ADM-082-03), 조건은 모두 선택. 기간은 호출일 양 끝 날짜 포함
 */
@Data
public class PdfAnalysisSearchCond {
    /** 시작일자 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromYmd;
    /** 종료일자 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toYmd;
}
