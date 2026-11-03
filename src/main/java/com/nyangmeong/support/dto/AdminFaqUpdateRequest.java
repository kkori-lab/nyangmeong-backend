package com.nyangmeong.support.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * [관리자 FAQ 수정 요청]
 * - PATCH /api/admin/faqs/{faqSn} (FN-ADM-041-02·03)
 * - 모두 선택, 보내지 않은 값(null)은 그대로
 * - useYn=false 면 비노출 (삭제 API 없음)
 */
public record AdminFaqUpdateRequest(
        @Pattern(regexp = ".*\\S.*", flags = Pattern.Flag.DOTALL, message = "카테고리를 선택해 주세요.") String faqClsfCd,
        @Pattern(regexp = ".*\\S.*", flags = Pattern.Flag.DOTALL, message = "질문을 입력해 주세요.") @Size(max = 200, message = "질문은 200자 이내로 입력해 주세요.") String qstnTtl,
        @Pattern(regexp = ".*\\S.*", flags = Pattern.Flag.DOTALL, message = "답변을 입력해 주세요.") @Size(max = 4000, message = "답변은 4000자 이내로 입력해 주세요.") String ansCn,
        @Min(value = 0, message = "정렬 순서는 0 이상이어야 합니다.") Integer sortSeq,
        Boolean useYn) {
}
