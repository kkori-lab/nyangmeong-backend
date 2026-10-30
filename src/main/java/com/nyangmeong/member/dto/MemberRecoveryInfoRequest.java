package com.nyangmeong.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [복구 안내 조회 요청]
 * - POST /api/members/recovery-info, 인증 확인 때 받은 복구 확인 권한 토큰이 곧 본인확인이다
 * - 토큰이 로그에 남지 않게 toString 을 막는다
 */
public record MemberRecoveryInfoRequest(
        @NotNull(message = "본인확인이 필요합니다. 이메일 인증을 다시 진행해 주세요.") Long emlCertSn,
        @NotBlank(message = "본인확인이 필요합니다. 이메일 인증을 다시 진행해 주세요.") @Size(max = 100) String rcvryAuthrtTkn) {

    @Override
    public String toString() {
        return "MemberRecoveryInfoRequest[***]";
    }
}
