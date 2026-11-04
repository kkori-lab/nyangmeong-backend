package com.nyangmeong.member.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 약관 목록·이력 항목]
 * - SCR-ADM-051 (FN-ADM-051-01·03), 내용(TRMS_CN)은 빼고 상세에서 준다
 */
@Data
public class AdminTermsSummary {
    private Long trmsSn;
    private String trmsFixIdntfKey;
    private String trmsVerId;
    private String trmsTtl;
    private boolean esntlAgreYn;
    private boolean useYn;
    private LocalDateTime regDt;
}
