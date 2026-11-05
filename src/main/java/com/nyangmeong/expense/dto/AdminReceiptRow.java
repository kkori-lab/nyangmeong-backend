package com.nyangmeong.expense.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 영수증 OCR 목록 항목]
 * - 분석 결과 내용(OCR 응답)은 주지 않는다 (결정 26)
 */
@Data
public class AdminReceiptRow {
    private Long recptSn;
    private Long mbrSn;
    private String mbrNcnm;
    private String ocrDmndSrcCd;
    private String ocrPrcsSttsCd;
    private LocalDateTime regDt;
}
