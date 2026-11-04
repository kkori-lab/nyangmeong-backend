package com.nyangmeong.global.external;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.regex.Pattern;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * [실제 SNS 제공자 호출]
 * - app.sns.client 가 없거나 real 이면 이 클라이언트를 쓴다 (가짜는 fake 로 정했을 때만)
 * - 인가 코드 → 액세스 토큰 → 사용자 정보 순서로 두 번 부른다
 * - 제공자가 인증하지 않은 이메일은 받지 않은 것으로 본다 (구글 email_verified, 카카오 is_email_verified·is_email_valid)
 * - 네이버·카카오는 콘솔의 "만 14세 이상" 필수 설정을 전제로 연령 확인을 true 로 본다, 구글은 연령을 주지 않아 false
 * - 어떤 실패든 SNS_AUTH_FAILED. 로그에는 제공자 오류 코드·설명만 남기고 토큰·인가 코드는 남기지 않는다
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.sns.client", havingValue = "real", matchIfMissing = true)
public class RealSnsOAuthClient implements SnsOAuthClient {

    private static final String NAVER_TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
    private static final String NAVER_ME_URL = "https://openapi.naver.com/v1/nid/me";
    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_ME_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_ME_URL = "https://openidconnect.googleapis.com/v1/userinfo";
    private static final int ERROR_SUMMARY_MAX_LENGTH = 300;
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    /** 오류 설명 속 code=값·token=값 (인가 코드·토큰). error_code= 처럼 앞에 단어가 붙은 필드 이름은 건드리지 않는다 */
    private static final Pattern CODE_VALUE = Pattern.compile("(?i)(\\b(?:code|token)\\s*[=:]\\s*)[^\\s,&;\"']+");

    private final SnsOAuthProperties properties;
    private final RestClient restClient;

    public RealSnsOAuthClient(SnsOAuthProperties properties) {
        this.properties = properties;
        RestClient.Builder builder = RestClient.builder();
        if (properties.getTimeoutMs() > 0) {
            Duration timeout = Duration.ofMillis(properties.getTimeoutMs());
            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                    HttpClient.newBuilder().connectTimeout(timeout).build());
            factory.setReadTimeout(timeout);
            builder.requestFactory(factory);
        }
        this.restClient = builder.build();
    }

    @Override
    public SnsProfile fetchProfile(SnsProvider provider, String code, String state) {
        SnsOAuthProperties.Provider keys = properties.of(provider);
        if (keys.getClientId().isBlank() || keys.getClientSecret().isBlank()) {
            log.warn("SNS 제공자 앱 키가 설정되지 않았습니다 : {}", provider);
            throw new CustomException(ErrorCode.SNS_AUTH_FAILED);
        }
        try {
            String accessToken = requestAccessToken(provider, keys, code, state);
            JsonNode me = requestMe(provider, accessToken);
            return toProfile(provider, me);
        } catch (CustomException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.warn("SNS 제공자 호출 실패 : {} HTTP {} {}", provider, e.getStatusCode().value(),
                    errorSummary(e.getResponseBodyAsByteArray()));
            throw new CustomException(ErrorCode.SNS_AUTH_FAILED);
        } catch (RestClientException | JacksonException | IllegalStateException e) {
            log.warn("SNS 제공자 호출 실패 : {} {}", provider, e.getClass().getSimpleName());
            throw new CustomException(ErrorCode.SNS_AUTH_FAILED);
        }
    }

    private String requestAccessToken(SnsProvider provider, SnsOAuthProperties.Provider keys, String code,
            String state) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", keys.getClientId());
        form.add("client_secret", keys.getClientSecret());
        form.add("code", code);
        String url = switch (provider) {
        case NAVER -> {
            form.add("state", state == null ? "" : state);
            yield NAVER_TOKEN_URL;
        }
        case KAKAO -> {
            form.add("redirect_uri", keys.getRedirectUri());
            yield KAKAO_TOKEN_URL;
        }
        case GOOGLE -> {
            form.add("redirect_uri", keys.getRedirectUri());
            yield GOOGLE_TOKEN_URL;
        }
        };
        byte[] body = restClient.post().uri(url).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form).retrieve().body(byte[].class);
        String accessToken = text(read(body), "access_token");
        if (accessToken == null) {
            /** 네이버는 실패도 HTTP 200 에 error 를 담아 준다 */
            log.warn("SNS 액세스 토큰 발급 실패 : {} {}", provider, errorSummary(body));
            throw new CustomException(ErrorCode.SNS_AUTH_FAILED);
        }
        return accessToken;
    }

    private JsonNode requestMe(SnsProvider provider, String accessToken) {
        String url = switch (provider) {
        case NAVER -> NAVER_ME_URL;
        case KAKAO -> KAKAO_ME_URL;
        case GOOGLE -> GOOGLE_ME_URL;
        };
        return read(restClient.get().uri(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).retrieve()
                .body(byte[].class));
    }

    /**
     * 사용자 정보 응답 → 회원 정보 : 인증되지 않은 이메일은 null
     */
    static SnsProfile toProfile(SnsProvider provider, JsonNode me) {
        String accountId;
        String email;
        String mblTelno = null;
        switch (provider) {
        case NAVER -> {
            JsonNode body = me.path("response");
            accountId = text(body, "id");
            email = text(body, "email");
            mblTelno = text(body, "mobile");
        }
        case KAKAO -> {
            JsonNode account = me.path("kakao_account");
            accountId = text(me, "id");
            boolean verified = account.path("is_email_verified").asBoolean(false)
                    && account.path("is_email_valid").asBoolean(false);
            email = verified ? text(account, "email") : null;
            mblTelno = text(account, "phone_number");
        }
        default -> {
            accountId = text(me, "sub");
            email = me.path("email_verified").asBoolean(false) ? text(me, "email") : null;
        }
        }
        if (accountId == null) {
            throw new CustomException(ErrorCode.SNS_AUTH_FAILED);
        }
        return new SnsProfile(provider, accountId, email, mblTelno, provider != SnsProvider.GOOGLE);
    }

    /**
     * 제공자 오류 응답 요약 : error·error_description(카카오 error_code, 네이버 사용자 정보 message 포함)만 꺼낸다
     * - 토큰이 실린 성공 응답이나 JSON 이 아닌 본문은 내용을 남기지 않는다
     */
    static String errorSummary(byte[] body) {
        if (body == null || body.length == 0) {
            return "(본문 없음)";
        }
        JsonNode node;
        try {
            node = JSON_MAPPER.readTree(body);
        } catch (JacksonException e) {
            return "(JSON 아님)";
        }
        StringBuilder summary = new StringBuilder();
        for (String field : new String[] { "error", "error_code", "error_description", "message", "resultcode" }) {
            String value = text(node, field);
            if (value != null) {
                summary.append(field).append('=').append(value).append(' ');
            }
        }
        /** 카카오는 error_description 에 인가 코드를 실어 보낸다(... for code=값) : 값을 가린다 */
        String result = summary.isEmpty() ? "(오류 필드 없음)"
                : CODE_VALUE.matcher(summary.toString().strip()).replaceAll("$1***");
        return result.length() > ERROR_SUMMARY_MAX_LENGTH ? result.substring(0, ERROR_SUMMARY_MAX_LENGTH) : result;
    }

    private static JsonNode read(byte[] body) {
        if (body == null) {
            throw new CustomException(ErrorCode.SNS_AUTH_FAILED);
        }
        return JSON_MAPPER.readTree(body);
    }

    /** 문자열 값, 없거나 비었으면 null (숫자 ID 도 문자열로) */
    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asString();
        return text == null || text.isBlank() ? null : text;
    }
}
