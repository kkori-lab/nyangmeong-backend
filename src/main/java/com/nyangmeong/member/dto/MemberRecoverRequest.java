package com.nyangmeong.member.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [기존 정보 복구 요청]
 * - POST /api/members/recovery, 복구 확인 권한 토큰 + 빠진 필수 약관 동의 + (보관 번호가 없을 때만) 휴대전화번호
 * - 이미 같은 버전으로 동의한 약관은 보내지 않아도 된다
 * - 토큰이 로그에 남지 않게 toString 을 막는다
 */
public record MemberRecoverRequest(
        @NotNull(message = "본인확인이 필요합니다. 이메일 인증을 다시 진행해 주세요.") Long emlCertSn,
        @NotBlank(message = "본인확인이 필요합니다. 이메일 인증을 다시 진행해 주세요.") @Size(max = 100) String rcvryAuthrtTkn,
        @Valid List<TermsAgreeRequest> trmsAgres, String mblTelno) {

    @Override
    public String toString() {
        return "MemberRecoverRequest[***]";
    }
}
