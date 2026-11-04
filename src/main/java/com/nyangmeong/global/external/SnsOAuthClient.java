package com.nyangmeong.global.external;

/**
 * [SNS 인가 코드 → 회원 정보]
 * - 구현은 둘 : RealSnsOAuthClient(기본), FakeSnsOAuthClient(app.sns.client=fake)
 * - 코드가 틀리거나 제공자 호출이 실패하면 SNS_AUTH_FAILED
 */
public interface SnsOAuthClient {

    SnsProfile fetchProfile(SnsProvider provider, String code, String state);
}
