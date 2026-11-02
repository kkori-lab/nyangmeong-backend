package com.nyangmeong.global.external;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [외부 API 호출 로그 VO]
 * - EXTL_API_CALL_LOG 1:1
 */
@Data
public class ExternalApiCallLog {
    /** 외부API호출로그일련번호 */
    private Long extlApiCallLogSn;
    /** 외부API구분공통코드 */
    private String extlApiSeCd;
    /** 호출결과공통코드 */
    private String callRsltCd;
    /** 회원일련번호 */
    private Long mbrSn;
    /** 호출일시 */
    private LocalDateTime callDt;
    /** 응답밀리초 */
    private Long rspnsMs;
    /** HTTP상태값 */
    private Integer httpSttsVl;
    /** 입력토큰수 */
    private Long inptTknCnt;
    /** 출력토큰수 */
    private Long otptTknCnt;
    /** 실패사유 */
    private String failRsn;
    /** 등록일시 */
    private LocalDateTime regDt;

    /**
     * 통지 요청으로 로그 생성
     */
    public static ExternalApiCallLog of(Long mbrSn, ExternalApiCallRequest req) {
        ExternalApiCallLog log = new ExternalApiCallLog();
        log.setExtlApiSeCd(req.extlApiSeCd());
        log.setCallRsltCd(req.callRsltCd());
        log.setMbrSn(mbrSn);
        log.setCallDt(req.callDt());
        log.setRspnsMs(req.rspnsMs());
        log.setHttpSttsVl(req.httpSttsVl());
        log.setInptTknCnt(req.inptTknCnt());
        log.setOtptTknCnt(req.otptTknCnt());
        log.setFailRsn(req.failRsn());
        return log;
    }
}
