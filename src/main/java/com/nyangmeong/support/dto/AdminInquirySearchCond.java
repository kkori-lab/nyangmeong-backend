package com.nyangmeong.support.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

/**
 * [관리자 1:1 문의 검색 조건]
 * - GET /api/admin/inquiries (FN-ADM-042-02)
 * - inqryCtgr : DISPUTE·ERROR·ETC (InquiryCategory)
 */
@Data
public class AdminInquirySearchCond {
    private String inqryTypeCd;
    private String inqryCtgr;
    private String inqryPrcsSttsCd;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate bgngYmd;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endYmd;
    private Boolean inclDelYn;
}
