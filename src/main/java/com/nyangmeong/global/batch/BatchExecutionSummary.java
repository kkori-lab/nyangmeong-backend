package com.nyangmeong.global.batch;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [배치 실행 이력 목록 응답]
 */
@Data
public class BatchExecutionSummary {
    /** 배치실행이력일련번호 */
    private Long btchExcnHstrySn;
    /** 실행시작일시 */
    private LocalDateTime excnBgngDt;
    /** 실행종료일시 */
    private LocalDateTime excnEndDt;
    /** 처리건수 */
    private Integer prcsCnt;
    /** 실패사유 */
    private String failRsn;
    /** 실행구분공통코드 */
    private String excnSeCd;
    /** 실행결과공통코드 */
    private String excnRsltCd;
    /** 실행한 관리자 닉네임 */
    private String mbrNcnm;
}
