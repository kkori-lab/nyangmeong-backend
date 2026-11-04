package com.nyangmeong.community.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [펫돌봄 매칭 상태 이력 VO]
 * - PET_PRTC_MTCH_STTS_HSTRY 1:1
 */
@Data
public class CareMatchStatusHistory {
    private Long petPrtcMtchSttsHstrySn;
    private Long petPrtcMtchSn;
    private Long mbrSn;
    private Long mtchCndVerSn;
    private String bfrSttsCd;
    private String aftrSttsCd;
    private String chgRsn;
    private LocalDateTime regDt;
}
