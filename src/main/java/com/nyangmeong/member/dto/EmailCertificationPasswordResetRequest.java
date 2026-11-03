package com.nyangmeong.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * [비밀번호 재설정 요청]
 * - POST /api/email-certifications/{emlCertSn}/password-reset, 인증 확인 때 받은 재설정 권한 토큰으로만 바꾼다
 * - 비밀번호는 BCrypt 한계(72바이트) 때문에 공백 없는 ASCII 로 제한, 원문이 로그에 남지 않게 toString 을 막는다
 */
public record EmailCertificationPasswordResetRequest(
        @NotBlank(message = "본인확인이 필요합니다. 이메일 인증을 다시 진행해 주세요.") @Size(max = 100) String pswdInitTkn,
        @NotBlank(message = "새 비밀번호를 입력해 주세요.") @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]{8,64}$", message = "비밀번호는 8자 이상, 문자와 숫자를 함께 입력해 주세요.") String pswd,
        @NotBlank(message = "새 비밀번호를 다시 입력해 주세요.") String pswdCnfrm) {

    @Override
    public String toString() {
        return "EmailCertificationPasswordResetRequest[***]";
    }
}
