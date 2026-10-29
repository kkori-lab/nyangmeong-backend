package com.nyangmeong.pet.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [공동양육초대 VO]
 * - JNT_BRUP_INVTN 1:1
 */
@Data
public class JntBrupInvtn {
    private Long jntBrupInvtnSn;
    private Long sndrMbrSn;
    private Long rcvrMbrSn;
    private String invtnSttsCd;
    private String invtnTrgtEmlEnc;
    private String invtnTrgtEmlHmac;
    private LocalDateTime invtnExpryDt;
    private LocalDateTime acptRfslRspnsDt;
    private String invtnTknHash;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
    private String mdfrId;
}
