package com.nyangmeong.expense.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

/**
 * [관리자 영수증 OCR 현황 검색 조건]
 * - SCR-ADM-082, 조건은 모두 선택. 기간은 등록일 양 끝 날짜 포함
 * - 건수 묶음(summary)은 기간·출처만, 목록은 상태까지 건다
 */
@Data
public class AdminReceiptSearchCond {
    /** 시작일자 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromYmd;
    /** 종료일자 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toYmd;
    /** OCR요청출처공통코드 (AIMG07) */
    private String ocrDmndSrcCd;
    /** OCR처리상태공통코드 (AIMG01) */
    private String ocrPrcsSttsCd;
}
