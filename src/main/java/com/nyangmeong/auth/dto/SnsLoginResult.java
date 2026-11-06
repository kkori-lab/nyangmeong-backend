package com.nyangmeong.auth.dto;

/**
 * [SNS 로그인 처리 결과]
 * - loginMbrSn : 로그인에 성공했을 때만 회원번호, 컨트롤러가 이 값이 있을 때만 로그인 쿠키를 발급한다
 */
public record SnsLoginResult(Long loginMbrSn, SnsLoginResponse response) {

    public static SnsLoginResult login(Long mbrSn, SnsLoginResponse response) {
        return new SnsLoginResult(mbrSn, response);
    }

    public static SnsLoginResult notLoggedIn(SnsLoginResponse response) {
        return new SnsLoginResult(null, response);
    }
}
