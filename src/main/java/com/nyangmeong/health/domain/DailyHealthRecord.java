package com.nyangmeong.health.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [일상건강기록 VO]
 * - DAILY_HLTH_RCD 1:1
 */
@Data
public class DailyHealthRecord {

    /** 일상건강기록일련번호 */
    private Long dailyHlthRcdSn;
    /** 반려동물일련번호 */
    private Long petSn;
    /** 일상기록지표공통코드(HLTG09) */
    private String dailyRcdIdctCd;
    /** 배변상태공통코드(HLTG10) */
    private String crsdSttsCd;
    /** 기록수치 */
    private BigDecimal rcdNvl;
    /** 기록일시 */
    private LocalDateTime rcdDt;
    /** 삭제여부 */
    private String delYn;
    /** 등록일시 */
    private LocalDateTime regDt;
    /** 수정일시 */
    private LocalDateTime mdfcnDt;
    /** 등록자ID */
    private String rgtrId;
    /** 수정자ID */
    private String mdfrId;
}
