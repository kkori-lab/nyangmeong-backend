package com.nyangmeong.health.domain;

/**
 * [검사결과지 분석 결과 구분]
 * - InspectionAnalysisResponse.anlysRslt 값 : 실패도 200으로 주고 이 값으로 문구 선택
 */
public enum AnalysisResult {
    SUCCESS,
    NOT_RECOGNIZED,
    TEMPORARY_ERROR
}
