package com.nyangmeong.pet.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 회원 상세 포인트 원장 한 줄]
 * - SCR-ADM-011, 최신순
 */
@Data
public class AdminPointLedgerSummary {
    /** 포인트원장일련번호 */
    private Long pntGnlgrSn;
    /** 포인트거래유형공통코드 */
    private String pntDlngTypeCd;
    /** 포인트거래유형명 */
    private String pntDlngTypeCdNm;
    /** 거래포인트 */
    private Integer dlngPnt;
    /** 거래후잔액포인트 */
    private Integer dlngAftrBlncPnt;
    /** 메모내용 */
    private String memoCn;
    /** 등록일시 */
    private LocalDateTime regDt;
}
