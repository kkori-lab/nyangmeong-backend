package com.nyangmeong.health.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [검진기록 VO]
 * - CKUP_RCD 1:1
 */
@Data
public class CheckupRecord {

    /** 검진기록일련번호 */
    private Long ckupRcdSn;
    /** 영수증일련번호 */
    private Long recptSn;
    /** 일정일련번호 */
    private Long schdlSn;
    /** 검진유형공통코드(HLTG01) */
    private String ckupTypeCd;
    /** 검진기록상태공통코드(HLTG02) */
    private String ckupRcdSttsCd;
    /** 기록출처공통코드(HLTG03) */
    private String rcdSrcCd;
    /** 병원명 */
    private String hsptlNm;
    /** 검진내용 */
    private String ckupCn;
    /** 검진메모내용 */
    private String ckupMemoCn;
    /** 등록일시 */
    private LocalDateTime regDt;
    /** 수정일시 */
    private LocalDateTime mdfcnDt;
    /** 등록자ID */
    private String rgtrId;
    /** 수정자ID */
    private String mdfrId;
}
