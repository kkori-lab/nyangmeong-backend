package com.nyangmeong.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [대화 메시지 추가 요청]
 * - cmptnYn 이 null 이면 true
 * - cnvrsCn 은 AI 답변 상한(10,000자)까지 받고, 사용자 질문 1,000자는 서비스가 역할별로 검사
 */
public record ConversationMessageCreateRequest(
        @NotBlank String roleCd,
        @NotBlank @Size(max = 10000) String cnvrsCn,
        Boolean cmptnYn) {
}
