package com.nyangmeong.health.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [처방 VO]
 * - PRSC 1:1
 */
@Data
public class Prescription {

    /** 처방일련번호 */
    private Long prscSn;
    /** 검진기록일련번호 */
    private Long ckupRcdSn;
    /** 처방상태공통코드(HLTG06) */
    private String prscSttsCd;
    /** 약품명 */
    private String mdcnNm;
    /** 1일복용횟수 */
    private Integer dayMdcnNmtm;
    /** 복용시작일자 */
    private LocalDate mdcnBgngYmd;
    /** 복용종료일자 */
    private LocalDate mdcnEndYmd;
    /** 케어리스트반영여부 */
    private String careRfltYn;
    /** 등록일시 */
    private LocalDateTime regDt;
    /** 수정일시 */
    private LocalDateTime mdfcnDt;
    /** 등록자ID */
    private String rgtrId;
    /** 수정자ID */
    private String mdfrId;
    /** 삭제여부 */
    private String delYn;
}
