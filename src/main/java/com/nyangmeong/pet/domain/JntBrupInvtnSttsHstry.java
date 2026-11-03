package com.nyangmeong.pet.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [공동양육초대상태이력 VO]
 * - JNT_BRUP_INVTN_STTS_HSTRY 1:1
 */
@Data
public class JntBrupInvtnSttsHstry {
    private Long jntBrupInvtnSttsHstrySn;
    private Long jntBrupInvtnSn;
    private Long mbrSn;
    private String bfrSttsCd;
    private String aftrSttsCd;
    private String chgRsn;
    private LocalDateTime regDt;
}
