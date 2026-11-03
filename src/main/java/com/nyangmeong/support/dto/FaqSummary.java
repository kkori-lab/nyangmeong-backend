package com.nyangmeong.support.dto;

import lombok.Data;

/**
 * [FAQ 목록 항목]
 * - GET /api/faqs (FN-CSC-002-01)
 * - 답변까지 담아 아코디언을 목록 안에서 펼친다 (FN-CSC-002-04)
 */
@Data
public class FaqSummary {
    private Long faqSn;
    private String faqClsfCd;
    private String faqClsfCdNm;
    private String qstnTtl;
    private String ansCn;
}
