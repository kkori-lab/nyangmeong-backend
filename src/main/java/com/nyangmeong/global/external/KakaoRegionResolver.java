package com.nyangmeong.global.external;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * [카카오 로컬 좌표 → 행정구역코드 변환]
 * - GET /v2/local/geo/coord2regioncode.json, Authorization: KakaoAK {REST API 키}
 * - 지역 표(RGN)가 행정동 코드라서 행정동(H)을 먼저 쓰고, 없으면 법정동(B), 결과가 비면 null
 * - 호출 실패(키 없음·HTTP 오류·시간 초과·응답 형식 오류)는 null 이 아니라 502 : 전북 밖 오류로 잘못 보이지 않게 함
 * - 호출마다 외부 API 호출 로그(KAKAO_LOCAL)에 남김 : 로그 저장 실패는 변환을 막지 않음
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.region.client", havingValue = "kakao")
public class KakaoRegionResolver implements RegionResolver {

    private static final String COORD2REGION_PATH = "/v2/local/geo/coord2regioncode.json";
    private static final String TYPE_ADMIN = "H";
    private static final String TYPE_LAW = "B";
    private static final int ADMDST_CD_LENGTH = 10;
    private static final int FAIL_RSN_MAX_LENGTH = 1000;

    private final KakaoLocalProperties properties;
    private final ExternalApiUsageService externalApiUsageService;
    private final RestClient restClient;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public KakaoRegionResolver(KakaoLocalProperties properties, ExternalApiUsageService externalApiUsageService) {
        this.properties = properties;
        this.externalApiUsageService = externalApiUsageService;
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.getBaseUrl());
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
    public String resolveAdmdstCd(BigDecimal lat, BigDecimal lot) {
        if (properties.getRestApiKey() == null || properties.getRestApiKey().isBlank()) {
            log.warn("카카오 로컬 API 키가 설정되지 않았습니다");
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR);
        }
        LocalDateTime calledAt = LocalDateTime.now();
        long startedAt = System.currentTimeMillis();
        try {
            byte[] body = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(COORD2REGION_PATH)
                            .queryParam("x", lot.toPlainString())
                            .queryParam("y", lat.toPlainString())
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.getRestApiKey())
                    .retrieve()
                    .body(byte[].class);
            String admdstCd = parse(jsonMapper.readTree(body));
            record(calledAt, startedAt, ExternalApiCallResult.SUCCESS, 200, null);
            return admdstCd;
        } catch (RestClientResponseException e) {
            record(calledAt, startedAt, ExternalApiCallResult.FAIL, e.getStatusCode().value(),
                    "HTTP " + e.getStatusCode().value());
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR);
        } catch (ResourceAccessException e) {
            boolean isTimeout = e.getCause() instanceof SocketTimeoutException
                    || e.getCause() instanceof HttpTimeoutException;
            record(calledAt, startedAt, isTimeout ? ExternalApiCallResult.TIMEOUT : ExternalApiCallResult.FAIL, null,
                    isTimeout ? "시간 초과" : "연결 실패");
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR);
        } catch (RuntimeException e) {
            record(calledAt, startedAt, ExternalApiCallResult.FAIL, 200, "응답 형식 오류");
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /**
     * 응답에서 행정구역코드 고르기 : 행정동 우선, 없으면 법정동, 문서가 비면 null
     */
    private String parse(JsonNode root) {
        String lawCode = null;
        for (JsonNode doc : root.get("documents")) {
            String code = doc.get("code").asString();
            if (code == null || code.length() != ADMDST_CD_LENGTH) {
                continue;
            }
            String type = doc.get("region_type").asString();
            if (TYPE_ADMIN.equals(type)) {
                return code;
            }
            if (TYPE_LAW.equals(type) && lawCode == null) {
                lawCode = code;
            }
        }
        return lawCode;
    }

    /**
     * 호출 로그 저장
     */
    private void record(LocalDateTime calledAt, long startedAt, ExternalApiCallResult result, Integer httpStatus,
            String failRsn) {
        try {
            String rsn = failRsn == null || failRsn.length() <= FAIL_RSN_MAX_LENGTH ? failRsn
                    : failRsn.substring(0, FAIL_RSN_MAX_LENGTH);
            externalApiUsageService.record(null, new ExternalApiCallRequest(
                    ExternalApiType.KAKAO_LOCAL.getCode(), result.getCode(), calledAt,
                    System.currentTimeMillis() - startedAt, httpStatus, null, null, rsn));
        } catch (RuntimeException e) {
            log.warn("카카오 로컬 호출 로그 저장에 실패했습니다: {}", e.getClass().getSimpleName());
        }
    }
}
