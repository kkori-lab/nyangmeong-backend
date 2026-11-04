package com.nyangmeong.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [SNS 로그인 요청]
 * - POST /api/auth/sns-login
 * - code : 프론트가 제공자 인가 창에서 받은 인가 코드
 * - state : 네이버 토큰 교환에 필요, 서버는 확인하지 않고 그대로 넘긴다 (CSRF 확인은 프론트)
 */
public record SnsLoginRequest(
        @NotBlank(message = "SNS 제공자를 보내 주세요.") @Size(max = 8) String snsOffrCd,
        @NotBlank(message = "인가 코드를 보내 주세요.") @Size(max = 2000) String code,
        @Size(max = 200) String state) {
}
