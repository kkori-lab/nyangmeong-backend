package com.nyangmeong.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * [비밀번호 변경 요청]
 * - PATCH /api/members/me/password, 로그인 상태에서 현재 비밀번호를 다시 받아 바꾼다 (FN-MYP-040-06)
 * - 새 비밀번호 규칙은 재설정과 같다(BCrypt 한계 72바이트 때문에 공백 없는 ASCII), 원문이 로그에 남지 않게 toString 을 막는다
 */
public record PasswordChangeRequest(
        @NotBlank(message = "현재 비밀번호를 입력해 주세요.") String currentPswd,
        @NotBlank(message = "새 비밀번호를 입력해 주세요.") @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]{8,64}$", message = "비밀번호는 8자 이상, 문자와 숫자를 함께 입력해 주세요.") String pswd,
        @NotBlank(message = "새 비밀번호를 다시 입력해 주세요.") String pswdCnfrm) {

    @Override
    public String toString() {
        return "PasswordChangeRequest[***]";
    }
}
