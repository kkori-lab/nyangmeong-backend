package com.nyangmeong.support.dto;

import lombok.Data;

/**
 * [문의 첨부 이미지 항목]
 * - 문의 작성·수정·상세 응답의 imgs[] (FN-CSC-003-09·12·15)
 * - 이미지 조회 경로는 GET /api/inquiries/{otoInqrySn}/images/{atchFileSn}
 */
@Data
public class InquiryImageSummary {
    private Long atchFileSn;
    private Integer sortSeq;
    private String orgnlFileNm;
}
