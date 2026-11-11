package com.nyangmeong.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [SNS 계정 연동 요청]
 * - POST /api/members/me/sns-links
 * - code : 프론트가 제공자 인가 창에서 받은 인가 코드
 * - state : 네이버 토큰 교환에 필요, 서버는 확인하지 않고 그대로 넘긴다 (CSRF 확인은 프론트)
 */
public record MemberSnsLinkRequest(
        @NotBlank(message = "SNS 제공자를 보내 주세요.") @Size(max = 8) String snsOffrCd,
        @NotBlank(message = "인가 코드를 보내 주세요.") @Size(max = 2000) String code,
        @Size(max = 200) String state) {
}
