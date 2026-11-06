package com.nyangmeong.global.external;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 검사결과지 분석 실패 목록 행]
 * - 분석 결과 내용(건강기록)은 주지 않는다 (결정 26)
 */
@Data
public class PdfAnalysisFailureRow {
    /** 외부API호출로그일련번호 */
    private Long extlApiCallLogSn;
    /** 호출결과공통코드 */
    private String callRsltCd;
    /** 실패사유 */
    private String failRsn;
    /** 호출일시 */
    private LocalDateTime callDt;
}
