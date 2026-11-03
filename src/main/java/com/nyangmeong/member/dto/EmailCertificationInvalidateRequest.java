package com.nyangmeong.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [이메일 인증 무효화 요청]
 * - POST /api/email-certifications/{emlCertSn}/invalidate, 일련번호가 순번이라 이메일이 맞아야 무효화한다
 */
public record EmailCertificationInvalidateRequest(
        @NotBlank(message = "이메일을 입력해 주세요.") @Email(message = "이메일 주소 형식을 확인해주세요.") @Size(max = 254) String certTrgtEml) {
}
