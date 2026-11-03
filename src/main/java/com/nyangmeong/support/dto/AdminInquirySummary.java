package com.nyangmeong.support.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 1:1 문의 목록 항목]
 * - GET /api/admin/inquiries (FN-ADM-042-01)
 * - inqryCtgr 는 조회 뒤 서비스가 유형으로 채움
 */
@Data
public class AdminInquirySummary {
    private Long otoInqrySn;
    private String inqryTypeCd;
    private String inqryTypeCdNm;
    private String inqryCtgr;
    private String inqryTtl;
    private Long mbrSn;
    private String mbrNcnm;
    private String inqryPrcsSttsCd;
    private String inqryPrcsSttsCdNm;
    private LocalDateTime regDt;
    private boolean delYn;
}
