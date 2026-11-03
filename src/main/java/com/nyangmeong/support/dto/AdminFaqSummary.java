package com.nyangmeong.support.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 FAQ 목록 항목]
 * - GET /api/admin/faqs (FN-ADM-041-01)
 */
@Data
public class AdminFaqSummary {
    private Long faqSn;
    private String faqClsfCd;
    private String faqClsfCdNm;
    private String qstnTtl;
    private Integer sortSeq;
    private boolean useYn;
    private LocalDateTime mdfcnDt;
}
