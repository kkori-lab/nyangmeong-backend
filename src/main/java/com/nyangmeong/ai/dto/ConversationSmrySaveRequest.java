package com.nyangmeong.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * [대화 요약 저장 요청]
 * - Python AI 서버가 긴 대화의 앞부분을 요약해 저장할 때 쓴다
 * - smryEndMsgSeq 는 요약에 넣은 마지막 메시지의 msgSeq
 */
public record ConversationSmrySaveRequest(
        @NotBlank @Size(max = 2000) String smryCn,
        @NotNull @Positive Integer smryEndMsgSeq) {
}
