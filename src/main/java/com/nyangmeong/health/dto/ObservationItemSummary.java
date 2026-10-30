package com.nyangmeong.health.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

/**
 * [관찰 항목 목록 행]
 * - 대표 사진·항목명·부위·현재 값·직전 대비 증감·최근 관찰일 (FN-HLT-002-15)
 * - 항목 정보는 매퍼가, 회차 요약은 서비스가 채움
 */
@Data
public class ObservationItemSummary {

    /** 관찰항목일련번호 */
    private Long obsrvArtclSn;
    /** 관찰항목명 */
    private String obsrvArtclNm;
    /** 관찰부위공통코드(HLTG16) */
    private String obsrvRegnCd;
    /** 관찰부위공통코드명 */
    private String obsrvRegnCdNm;
    /** 관찰부위상세명 */
    private String obsrvRegnDtlNm;
    /** 관찰상태공통코드(HLTG18) */
    private String obsrvSttsCd;
    /** 관찰종료일자 */
    private LocalDate obsrvEndYmd;
    /** 마지막 회차 번호 */
    private Integer cyclCnt;
    /** 마지막 회차 크기 - 크기를 입력한 그 이전 회차 크기 */
    private BigDecimal lastSzDiff;
    /** 첫 회차 */
    private ObservationCycleSummary firstCycle;
    /** 최근 회차 */
    private ObservationCycleSummary lastCycle;
}
