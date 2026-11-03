package com.nyangmeong.support.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [관리자 FAQ 등록 요청]
 * - POST /api/admin/faqs (FN-ADM-041-02)
 * - faqClsfCd : 공통코드 CSCG01 (FN-ADM-041-04)
 * - sortSeq : 중복 허용, 같으면 번호순
 */
public record AdminFaqCreateRequest(
        @NotBlank(message = "카테고리를 선택해 주세요.") String faqClsfCd,
        @NotBlank(message = "질문을 입력해 주세요.") @Size(max = 200, message = "질문은 200자 이내로 입력해 주세요.") String qstnTtl,
        @NotBlank(message = "답변을 입력해 주세요.") @Size(max = 4000, message = "답변은 4000자 이내로 입력해 주세요.") String ansCn,
        @NotNull(message = "정렬 순서를 입력해 주세요.") @Min(value = 0, message = "정렬 순서는 0 이상이어야 합니다.") Integer sortSeq,
        @NotNull(message = "사용 여부를 선택해 주세요.") Boolean useYn) {
}
