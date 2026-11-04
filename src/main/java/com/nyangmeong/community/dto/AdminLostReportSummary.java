package com.nyangmeong.community.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 실종제보 목록 항목]
 * - expryCrtrDt : 찾았어요 글은 null
 */
@Data
public class AdminLostReportSummary {
    private Long lostTpofSn;
    private Long pstSn;
    private String pstTtl;
    private String lostTpofSttsCd;
    private Long mbrSn;
    private String mbrNcnm;
    private LocalDateTime expryCrtrDt;
    private LocalDateTime regDt;
}
