package com.nyangmeong.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [관리자 회원 정지 해제 요청]
 */
public record AdminMemberReleaseRequest(
        @NotBlank(message = "해제 사유를 입력해 주세요.") @Size(max = 1000, message = "해제 사유는 1000자 이내로 입력해 주세요.") String rlsRsn) {
}
