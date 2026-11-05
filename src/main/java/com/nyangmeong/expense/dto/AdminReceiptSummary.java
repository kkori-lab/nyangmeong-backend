package com.nyangmeong.expense.dto;

import lombok.Data;

/**
 * [관리자 영수증 OCR 건수 묶음]
 * - 현재 상태 기준. 기간·출처만 걸고 상태 필터와 무관
 * - scsCnt : 인식성공·부분인식·저장완료, failCnt : 인식실패, prcsCnt : 분석중, errCnt : 일시오류
 * - 취소는 totCnt 에만 들어간다
 */
@Data
public class AdminReceiptSummary {
    private long totCnt;
    private long scsCnt;
    private long failCnt;
    private long prcsCnt;
    private long errCnt;
}
