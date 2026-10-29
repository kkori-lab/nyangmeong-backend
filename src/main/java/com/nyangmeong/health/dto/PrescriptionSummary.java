package com.nyangmeong.health.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * [처방약 목록 행]
 * - 처방약 탭 (FN-HLT-005-03) : 복용 중 우선·최신순
 */
@Data
public class PrescriptionSummary {

    /** 처방일련번호 */
    private Long prscSn;
    /** 검진기록일련번호 */
    private Long ckupRcdSn;
    /** 약품명 */
    private String mdcnNm;
    /** 복용시작일자 */
    private LocalDate mdcnBgngYmd;
    /** 복용종료일자 */
    private LocalDate mdcnEndYmd;
    /** 1일복용횟수 */
    private Integer dayMdcnNmtm;
    /** 처방상태공통코드(HLTG06) */
    private String prscSttsCd;
    /** 처방상태명 */
    private String prscSttsCdNm;
    /** 케어리스트반영여부 */
    private boolean careRfltYn;
}
