package com.nyangmeong.community.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [펫돌봄 모집 VO]
 * - PET_PRTC_RCRT 1:1
 */
@Data
public class CareRecruitment {
    private Long petPrtcRcrtSn;
    private Long pstSn;
    private Long rgnSn;
    private String rcrtSttsCd;
    private String cstUnitCd;
    private Integer cstAmt;
    private String rlsRgnNm;
    private String prtcPlcDaddrEnc;
    private LocalDateTime prtcBgngDt;
    private LocalDateTime prtcEndDt;
    private String schdlCnsltnPsbltyYn;
    private String utztnRulAgreYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
    private BigDecimal prtcPlcLat;
    private BigDecimal prtcPlcLot;
    private Long cfmtnPetPrtcMtchSn;
}
