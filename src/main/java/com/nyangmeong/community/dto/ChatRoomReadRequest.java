package com.nyangmeong.community.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * [채팅 읽음 처리 요청]
 * - 활성 대화창에 표시된 마지막 메시지 순서
 */
public record ChatRoomReadRequest(@NotNull @Min(0) Integer lastPrslMsgSeq) {
}
