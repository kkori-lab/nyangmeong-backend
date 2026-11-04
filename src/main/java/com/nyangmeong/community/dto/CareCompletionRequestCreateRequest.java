package com.nyangmeong.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [돌봄 완료 요청]
 * - 별도 입력 항목이 없고 중복 제출 방지 ID 만 받는다
 */
public record CareCompletionRequestCreateRequest(
        @NotBlank @Size(max = 100) String idmptDmndId) {
}
