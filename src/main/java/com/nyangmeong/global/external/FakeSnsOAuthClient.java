package com.nyangmeong.global.external;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

/**
 * [가짜 SNS 제공자] 제공자 앱 키 없이 로컬에서 확인할 때만 쓴다 (app.sns.client=fake 로 정했을 때만, 기본은 실제 호출)
 * - code 가 "fake-" 로 시작해야 한다. 아니면 SNS_AUTH_FAILED
 * - "fake-이름" : 계정 ID 는 제공자+code, 이메일은 이름@example.com (같은 이메일의 회원이 있으면 중복 확인에 쓸 수 있다)
 * - 이름이 -noemail 로 끝나면 이메일을 주지 않는다
 * - 번호는 네이버·카카오만 code 에서 만든 고정값, 구글은 주지 않는다. 연령 확인은 실제 클라이언트처럼 항상 false
 */
@Component
@ConditionalOnProperty(name = "app.sns.client", havingValue = "fake")
public class FakeSnsOAuthClient implements SnsOAuthClient {

    private static final String CODE_PREFIX = "fake-";
    private static final String NO_EMAIL_SUFFIX = "-noemail";

    @Override
    public SnsProfile fetchProfile(SnsProvider provider, String code, String state) {
        if (code == null || !code.startsWith(CODE_PREFIX) || code.length() == CODE_PREFIX.length()) {
            throw new CustomException(ErrorCode.SNS_AUTH_FAILED);
        }
        String name = code.substring(CODE_PREFIX.length());
        String email = name.endsWith(NO_EMAIL_SUFFIX) ? null : name + "@example.com";
        boolean phoneProvided = provider != SnsProvider.GOOGLE;
        String mblTelno = phoneProvided ? String.format("010-%04d-%04d", Math.abs(code.hashCode()) % 10000,
                Math.abs((code + provider.name()).hashCode()) % 10000) : null;
        return new SnsProfile(provider, provider.getCode() + ":" + code, email, mblTelno, false);
    }
}
