package com.nyangmeong.support.dto;

import java.time.LocalDateTime;

import com.nyangmeong.support.domain.Faq;

/**
 * [관리자 FAQ 상세 응답]
 * - GET·PATCH /api/admin/faqs/{faqSn}, POST /api/admin/faqs (FN-ADM-041-02·03)
 */
public record AdminFaqResponse(
        Long faqSn,
        String faqClsfCd,
        String qstnTtl,
        String ansCn,
        Integer sortSeq,
        boolean useYn,
        LocalDateTime regDt,
        LocalDateTime mdfcnDt) {

    public static AdminFaqResponse from(Faq faq) {
        return new AdminFaqResponse(faq.getFaqSn(), faq.getFaqClsfCd(), faq.getQstnTtl(), faq.getAnsCn(),
                faq.getSortSeq(), "Y".equals(faq.getUseYn()), faq.getRegDt(), faq.getMdfcnDt());
    }
}
