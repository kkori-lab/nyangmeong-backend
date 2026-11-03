package com.nyangmeong.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [대화 제목 수정 요청]
 * - 첫 답변 요약 제목을 Python AI 서버가 한 번 바꿀 때 쓴다 (사용자 제목 변경 아님)
 */
public record ConversationTitleUpdateRequest(
        @NotBlank @Size(max = 200) String cnvrsTtl) {
}
