package com.nyangmeong.global.external;

/**
 * [SNS 인가 코드 → 회원 정보]
 * - 구현은 둘 : RealSnsOAuthClient(기본), FakeSnsOAuthClient(app.sns.client=fake)
 * - 코드가 틀리거나 제공자 호출이 실패하면 SNS_AUTH_FAILED
 * - requestOrigin : 브라우저 요청의 Origin 헤더(없으면 null). 실제 클라이언트가 토큰 교환의 redirect_uri 를 정하는 데 쓴다
 */
public interface SnsOAuthClient {

    SnsProfile fetchProfile(SnsProvider provider, String code, String state, String requestOrigin);

    /** Origin 을 모를 때 : redirect_uri 는 설정값 → app.front-base-url 순서로 정한다 */
    default SnsProfile fetchProfile(SnsProvider provider, String code, String state) {
        return fetchProfile(provider, code, state, null);
    }
}
