package com.nyangmeong.global.external;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [외부 API 실패 목록 응답]
 * - 요청·응답 본문은 저장도 응답도 하지 않음
 */
@Data
public class ExternalApiFailureSummary {
    /** 외부API호출로그일련번호 */
    private Long extlApiCallLogSn;
    /** 호출일시 */
    private LocalDateTime callDt;
    /** 외부API구분공통코드 */
    private String extlApiSeCd;
    /** 호출결과공통코드 */
    private String callRsltCd;
    /** 실패사유 */
    private String failRsn;

    public String getExtlApiNm() {
        return ExternalApiType.labelOf(extlApiSeCd);
    }
}
