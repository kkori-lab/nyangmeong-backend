package com.nyangmeong.community.dto;

import jakarta.validation.constraints.NotNull;

/**
 * [채팅방 생성 요청]
 * - 타인의 모집중 펫돌봄 게시글, 같은 게시글·지원자 조합에 방이 있으면 기존 방을 돌려준다
 */
public record ChatRoomCreateRequest(@NotNull Long pstSn) {
}
