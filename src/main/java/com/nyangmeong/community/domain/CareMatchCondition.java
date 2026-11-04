package com.nyangmeong.community.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [매칭 조건 버전 VO]
 * - MTCH_CND_VER 1:1
 */
@Data
public class CareMatchCondition {
    private Long mtchCndVerSn;
    private Long petPrtcMtchSn;
    private Long mbrSn;
    private String cstUnitCd;
    private Integer cndVerNo;
    private Integer acrdCstAmt;
    private String acrdPrtcCn;
    private LocalDateTime acrdPrtcBgngDt;
    private LocalDateTime acrdPrtcEndDt;
    private String schdlCnsltnPsbltyYn;
    private LocalDateTime regDt;
}
