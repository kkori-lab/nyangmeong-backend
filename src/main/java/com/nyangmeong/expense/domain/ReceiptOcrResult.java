package com.nyangmeong.expense.domain;

/**
 * [Python AI 서버가 보고하는 영수증 분석 결과]
 * - RECOGNIZED -> 인식성공·부분인식·인식실패를 Java 가 OCR 응답으로 판정
 * - TIMEOUT·API_ERROR -> 일시오류, 이름을 RECPT_STTS_HSTRY.CHG_RSN 에 저장 (FN-AIM-010-01·07)
 */
public enum ReceiptOcrResult {
    RECOGNIZED,
    UNRECOGNIZED,
    TIMEOUT,
    API_ERROR,
    CANCELED
}
