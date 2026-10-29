package com.nyangmeong.health.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * [검진기록 목록 행]
 * - 검진기록 리스트·필터 칩 (FN-HLT-002-05·06)
 */
@Data
public class CheckupSummary {

    /** 검진기록일련번호 */
    private Long ckupRcdSn;
    /** 일정일련번호 */
    private Long schdlSn;
    /** 검진일 */
    private LocalDate ckupYmd;
    /** 병원명 */
    private String hsptlNm;
    /** 검진유형공통코드 */
    private String ckupTypeCd;
    /** 검진유형명 */
    private String ckupTypeCdNm;
    /** 연결 일정 삭제 여부 */
    private boolean schdlDelYn;
}
