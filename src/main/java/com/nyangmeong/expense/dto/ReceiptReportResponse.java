package com.nyangmeong.expense.dto;

/**
 * [영수증 분석 결과·상태 보고 결과]
 * - ocrPrcsSttsCd : 반영한 상태 AIMG01
 */
public record ReceiptReportResponse(Long recptSn, String ocrPrcsSttsCd) {
}
