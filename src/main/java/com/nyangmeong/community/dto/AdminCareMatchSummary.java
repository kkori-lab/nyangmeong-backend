package com.nyangmeong.community.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 펫돌봄 모집글 목록 항목]
 */
@Data
public class AdminCareMatchSummary {
    private Long petPrtcRcrtSn;
    private Long pstSn;
    private String pstTtl;
    private String rcrtSttsCd;
    private Long mbrSn;
    private String mbrNcnm;
    private BigDecimal cstAmt;
    private String cstUnitCd;
    private LocalDateTime regDt;
}
