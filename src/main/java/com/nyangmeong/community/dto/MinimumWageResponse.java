package com.nyangmeong.community.dto;

/**
 * [최저시급 조회 응답]
 * - GET /api/care-recruitments/minimum-wage (FN-CMU-004-08), 관리자 설정(SYS_STNG)의 현재 값
 * - lowstHrwgAplcnYr : 적용 연도, lowstHrwgAmt : 시간당 최저시급(원)
 */
public record MinimumWageResponse(String lowstHrwgAplcnYr, Long lowstHrwgAmt) {
}
