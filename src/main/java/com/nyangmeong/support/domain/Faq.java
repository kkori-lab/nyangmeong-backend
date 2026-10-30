package com.nyangmeong.support.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [자주 묻는 질문 VO]
 * - FAQ 1:1
 */
@Data
public class Faq {
    private Long faqSn;
    private String faqClsfCd;
    private String qstnTtl;
    private String ansCn;
    private Integer sortSeq;
    private String useYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
}
