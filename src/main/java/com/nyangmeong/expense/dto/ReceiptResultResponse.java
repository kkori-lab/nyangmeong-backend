package com.nyangmeong.expense.dto;

import com.nyangmeong.global.external.ReceiptOcrResponse;

/**
 * [영수증 분석 결과 다시 열기]
 * - Python POST /ai/receipts 응답 {recptSn, ocr} 에 반려동물·상태를 더한 모양 (FN-AIM-013-06)
 * - ocr : 저장된 OCR 응답 그대로, 인식성공·부분인식·저장완료만, 그 밖은 null
 * - failRsn : 일시 오류 사유(TIMEOUT·API_ERROR), 그 밖은 null
 */
public record ReceiptResultResponse(Long recptSn, Long petSn, String ocrPrcsSttsCd, String failRsn,
        ReceiptOcrResponse ocr) {
}
