package com.nyangmeong.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [관리자 1:1 문의 답변 등록·수정 요청]
 * - POST·PATCH /api/admin/inquiries/{otoInqrySn}/answer (FN-ADM-042-04·05)
 */
public record AdminInquiryAnswerRequest(
        @NotBlank(message = "답변을 입력해 주세요.") @Size(max = 4000, message = "답변은 4000자 이내로 입력해 주세요.") String ansCn) {
}
