package com.nyangmeong.support.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 1:1 문의 상태 이력 항목]
 * - GET /api/admin/inquiries/{otoInqrySn} 의 hstrys[] (FN-ADM-042-03)
 */
@Data
public class AdminInquiryHistorySummary {
    private String bfrSttsCd;
    private String bfrSttsCdNm;
    private String aftrSttsCd;
    private String aftrSttsCdNm;
    private Long mbrSn;
    private String mbrNcnm;
    private String chgRsn;
    private LocalDateTime regDt;
}
