package com.nyangmeong.global.external;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
 * [카카오 로컬 키워드 검색 클라이언트]
 * - GET /v2/local/search/keyword.json, Authorization: KakaoAK {REST API 키}
 * - 호출마다 외부 API 호출 로그(KAKAO_LOCAL)에 성공·실패·시간초과를 남김 : 로그 저장 실패는 검색을 막지 않음
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.kakao-local.client", havingValue = "kakao")
public class KakaoPlaceSearchClient implements PlaceSearchClient {

    private static final String KEYWORD_PATH = "/v2/local/search/keyword.json";
    private static final int FAIL_RSN_MAX_LENGTH = 1000;

    private final KakaoLocalProperties properties;
    private final ExternalApiUsageService externalApiUsageService;
    private final RestClient restClient;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public KakaoPlaceSearchClient(KakaoLocalProperties properties, ExternalApiUsageService externalApiUsageService) {
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
    public PlaceSearchPage search(PlaceSearchQuery query) {
        if (properties.getRestApiKey() == null || properties.getRestApiKey().isBlank()) {
            log.warn("카카오 로컬 API 키가 설정되지 않았습니다");
            throw new CustomException(ErrorCode.EXTERNAL_API_ERROR);
        }
        LocalDateTime calledAt = LocalDateTime.now();
        long startedAt = System.currentTimeMillis();
        try {
            byte[] body = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(KEYWORD_PATH)
                            .queryParam("query", query.query())
                            .queryParam("rect", rect(query))
                            .queryParam("x", query.cntrLot().toPlainString())
                            .queryParam("y", query.cntrLat().toPlainString())
                            .queryParam("sort", query.distanceFirst() ? "distance" : "accuracy")
                            .queryParam("page", query.page())
                            .queryParam("size", PAGE_SIZE)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.getRestApiKey())
                    .retrieve()
                    .body(byte[].class);
            PlaceSearchPage page = parse(jsonMapper.readTree(body));
            record(calledAt, startedAt, ExternalApiCallResult.SUCCESS, 200, null);
            return page;
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
     * 카카오 rect 값 : 왼쪽 경도, 아래 위도, 오른쪽 경도, 위쪽 위도
     */
    private String rect(PlaceSearchQuery query) {
        return query.swLot().toPlainString() + "," + query.swLat().toPlainString() + ","
                + query.neLot().toPlainString() + "," + query.neLat().toPlainString();
    }

    /**
     * 응답 본문을 한 쪽 결과로 변환
     */
    private PlaceSearchPage parse(JsonNode root) {
        JsonNode meta = root.get("meta");
        List<PlaceSearchItem> items = new ArrayList<>();
        for (JsonNode doc : root.get("documents")) {
            String roadAddr = doc.path("road_address_name").asString("");
            items.add(new PlaceSearchItem(
                    doc.get("id").asString(),
                    doc.get("place_name").asString(),
                    roadAddr.isBlank() ? doc.path("address_name").asString(null) : roadAddr,
                    doc.path("phone").asString(null),
                    doc.path("place_url").asString(null),
                    new BigDecimal(doc.get("y").asString()),
                    new BigDecimal(doc.get("x").asString()),
                    doc.path("category_name").asString("")));
        }
        return new PlaceSearchPage(items, meta.path("pageable_count").asInt(items.size()),
                meta.path("is_end").asBoolean(true));
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
