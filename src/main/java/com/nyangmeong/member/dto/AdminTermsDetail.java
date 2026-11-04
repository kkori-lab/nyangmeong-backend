package com.nyangmeong.member.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 약관 상세]
 * - SCR-ADM-051 (FN-ADM-051-02·03), 사용 안 함 버전도 내용과 함께
 */
@Data
public class AdminTermsDetail {
    private Long trmsSn;
    private String trmsFixIdntfKey;
    private String trmsVerId;
    private String trmsTtl;
    private String trmsCn;
    private boolean esntlAgreYn;
    private boolean useYn;
    private LocalDateTime regDt;
}
