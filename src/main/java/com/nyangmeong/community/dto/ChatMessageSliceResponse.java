package com.nyangmeong.community.dto;

import java.util.List;

/**
 * [채팅 메시지 조각 응답]
 * - 전체 건수를 세지 않는 커서 응답, content 는 최신순
 */
public record ChatMessageSliceResponse(List<ChatMessageSummary> content, boolean hasNext) {
}
