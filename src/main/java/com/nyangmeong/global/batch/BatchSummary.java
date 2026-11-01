package com.nyangmeong.global.batch;

import java.time.LocalDateTime;

/**
 * [배치 목록 응답]
 * - 마지막 실행이 없으면 last 항목은 null
 */
public record BatchSummary(String btchSeCd, String btchNm, LocalDateTime lastBgngDt, LocalDateTime lastEndDt,
        String lastRsltCd, Integer prcsCnt, String failRsn, boolean runningYn, boolean runnableYn) {

    /**
     * 배치 구분과 마지막 실행으로 응답 생성
     */
    public static BatchSummary from(BatchType type, BatchExecution last, boolean runningYn, boolean runnableYn) {
        if (last == null) {
            return new BatchSummary(type.getCode(), type.getLabel(), null, null, null, null, null, runningYn,
                    runnableYn);
        }
        return new BatchSummary(type.getCode(), type.getLabel(), last.getExcnBgngDt(), last.getExcnEndDt(),
                last.getExcnRsltCd(), last.getPrcsCnt(), last.getFailRsn(), runningYn, runnableYn);
    }
}
