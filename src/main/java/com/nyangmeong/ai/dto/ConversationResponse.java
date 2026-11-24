package com.nyangmeong.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [대화 상세 응답]
 * - messages 는 msgSeq 오름차순
 * - smryCn·smryEndMsgSeq 는 요약이 없으면 null (내부 API 전용)
 */
public record ConversationResponse(Long aiCnvrsRoomSn, Long petSn, String cnvrsTtl, LocalDateTime regDt,
        List<ConversationMessageResponse> messages, String smryCn, Integer smryEndMsgSeq) {
}
