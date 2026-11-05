package com.nyangmeong.global.external;

import lombok.Data;

/**
 * [관리자 검사결과지 분석 건수 묶음]
 * - totCnt : 요청, scsCnt : 성공, tmotCnt : 시간초과, errCnt : API 오류
 * - 검사결과지가 아닌 파일은 Python 이 성공으로 통지하므로 실패에 들어가지 않는다
 */
@Data
public class PdfAnalysisSummary {
    private long totCnt;
    private long scsCnt;
    private long tmotCnt;
    private long errCnt;
}
