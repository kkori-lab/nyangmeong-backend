package com.nyangmeong.global.batch;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [배치 실행 이력 VO]
 * - BTCH_EXCN_HSTRY 1:1
 */
@Data
public class BatchExecution {
    /** 배치실행이력일련번호 */
    private Long btchExcnHstrySn;
    /** 배치구분공통코드 */
    private String btchSeCd;
    /** 실행구분공통코드 */
    private String excnSeCd;
    /** 실행결과공통코드 */
    private String excnRsltCd;
    /** 회원일련번호 */
    private Long mbrSn;
    /** 실행시작일시 */
    private LocalDateTime excnBgngDt;
    /** 실행종료일시 */
    private LocalDateTime excnEndDt;
    /** 처리건수 */
    private Integer prcsCnt;
    /** 실패사유 */
    private String failRsn;
    /** 등록일시 */
    private LocalDateTime regDt;
    /** 수정일시 */
    private LocalDateTime mdfcnDt;

    /**
     * 실행중 이력 생성
     */
    public static BatchExecution start(BatchType type, BatchExecutionType executionType, Long mbrSn) {
        BatchExecution execution = new BatchExecution();
        execution.setBtchSeCd(type.getCode());
        execution.setExcnSeCd(executionType.getCode());
        execution.setExcnRsltCd(BatchExecutionResult.RUNNING.getCode());
        execution.setMbrSn(mbrSn);
        return execution;
    }
}
