package com.nyangmeong.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [매칭 요청]
 * - 조건은 받지 않고 요청 시점의 모집글 조건을 복사한다
 */
public record CareMatchCreateRequest(
        @NotNull Long otoChatRoomSn,
        @NotBlank @Size(max = 100) String idmptDmndId) {
}
