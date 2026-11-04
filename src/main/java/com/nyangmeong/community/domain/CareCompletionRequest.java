package com.nyangmeong.community.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [돌봄 완료 요청 회차 VO]
 * - PRTC_CMPTN_DMND_CYCL 1:1
 */
@Data
public class CareCompletionRequest {
    private Long prtcCmptnDmndCyclSn;
    private Long petPrtcMtchSn;
    private Long mtchCndVerSn;
    private Long mbrSn;
    private String prtcrIdntyRsltCd;
    private Integer dmndCycl;
    private String cmptnCn;
    private String idmptDmndId;
    private LocalDateTime prtcEndDt;
    private String splmntDmndRsn;
    private LocalDateTime prtcrIdntyDt;
    private String hnovCmptnIdntyYn;
    private String spclMttrCn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
}
