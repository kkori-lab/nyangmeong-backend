package com.nyangmeong.expense.dto;

import com.nyangmeong.expense.domain.ReceiptOcrResult;
import com.nyangmeong.global.external.ReceiptOcrResponse;

import jakarta.validation.constraints.NotNull;

/**
 * [영수증 분석 결과·상태 보고 요청]
 * - Python AI 서버 -> Java (FN-AIM-010-01·013-04)
 * - ocr : RECOGNIZED 필수, UNRECOGNIZED 는 있으면 저장, 일시 오류·취소는 무시
 */
public record ReceiptReportRequest(@NotNull ReceiptOcrResult ocrRslt, ReceiptOcrResponse ocr) {
}
