package com.nyangmeong.member.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * [회원가입 요청]
 * - POST /api/members, 이메일 가입과 SNS 가입 공통
 * - 이메일 가입 : emlCertSn·certNo·pswd·pswdCnfrm 필수, signupTkn 은 보내지 않는다
 * - SNS 가입 : signupTkn 필수, emlCertSn·certNo·pswd·pswdCnfrm 은 보내지 않는다 (경로별 필수·금지는 MemberJoinService 가 검사)
 * - 비밀번호는 BCrypt 한계(72바이트) 때문에 공백 없는 ASCII 로 제한
 */
public record MemberCreateRequest(
        @NotBlank(message = "가입 경로를 보내 주세요.") String joinPathCd,
        Long emlCertSn,
        @Pattern(regexp = "^\\d{6}$", message = "인증번호는 숫자 6자리로 입력해 주세요.") String certNo,
        /** POST /auth/sns-login 이 준 신규 가입 확인 토큰 */
        @Size(max = 2000) String signupTkn,
        @NotBlank(message = "휴대전화번호를 입력해 주세요.") @Size(max = 20, message = "휴대전화번호 형식을 확인해 주세요.") String mblTelno,
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]{8,64}$", message = "비밀번호는 8자 이상, 영문과 숫자를 함께 입력해 주세요.") String pswd,
        String pswdCnfrm,
        @NotEmpty(message = "약관 동의 내용을 보내 주세요.") @Valid List<TermsAgreeRequest> trmsAgres,
        /** 같은 가입 시도의 재요청 식별값 : 저장 컬럼이 없어 받기만 한다 (재요청은 인증·연동 기록으로 판별) */
        @Size(max = 100) String idmptDmndId) {
}
