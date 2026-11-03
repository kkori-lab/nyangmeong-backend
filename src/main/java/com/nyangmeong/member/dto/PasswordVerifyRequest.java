package com.nyangmeong.member.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * [현재 비밀번호 확인 요청]
 * - POST /api/members/me/password-verification, 회원정보 수정 진입 전 본인 확인 (FN-MYP-040-01)
 * - 형식 규칙은 적용하지 않고(로그인과 같음) 원문이 로그에 남지 않게 toString 을 막는다
 */
public record PasswordVerifyRequest(
        @NotBlank(message = "비밀번호를 입력해 주세요.") String pswd) {

    @Override
    public String toString() {
        return "PasswordVerifyRequest[***]";
    }
}
