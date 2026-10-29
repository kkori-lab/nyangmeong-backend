package com.nyangmeong.health.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [일상 건강기록 목록 응답]
 * - 그래프·오늘의 기록·날짜별 표·최신 체중 (FN-HLT-002-09·10·12·13)
 */
@Data
public class DailyHealthRecordSummary {

    /** 일상건강기록일련번호 */
    private Long dailyHlthRcdSn;
    /** 일상기록지표공통코드(HLTG09) */
    private String dailyRcdIdctCd;
    /** 기록수치 */
    private BigDecimal rcdNvl;
    /** 배변상태공통코드(HLTG10) */
    private String crsdSttsCd;
    /** 배변상태공통코드명 */
    private String crsdSttsCdNm;
    /** 기록일시 */
    private LocalDateTime rcdDt;
}
