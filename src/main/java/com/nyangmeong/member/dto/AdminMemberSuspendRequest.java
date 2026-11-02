package com.nyangmeong.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [관리자 회원 정지 요청]
 * - suspnDayCnt 가 null 이면 영구 정지
 */
public record AdminMemberSuspendRequest(
        Integer suspnDayCnt,
        @NotBlank(message = "정지 사유를 입력해 주세요.") @Size(max = 1000, message = "정지 사유는 1000자 이내로 입력해 주세요.") String suspnRsn) {
}
