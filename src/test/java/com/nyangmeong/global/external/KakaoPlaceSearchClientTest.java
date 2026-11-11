package com.nyangmeong.global.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.external.PlaceSearchClient.PlaceSearchPage;
import com.nyangmeong.global.external.PlaceSearchClient.PlaceSearchQuery;
import com.sun.net.httpserver.HttpServer;

/**
 * [검사 - 카카오 로컬 키워드 검색 클라이언트]
 * - JDK 내장 HTTP 서버를 카카오 대신 띄워 요청 주소·헤더, 응답 변환, 호출 로그를 확인
 */
class KakaoPlaceSearchClientTest {

    private static final String BODY = """
            {"meta":{"total_count":31,"pageable_count":31,"is_end":false},
             "documents":[
              {"id":"1001","place_name":"온누리동물병원","category_name":"가정,생활 > 반려동물 > 동물병원",
               "phone":"063-245-0002","address_name":"전북 전주시 완산구 효자동","road_address_name":"전북 전주시 완산구 효자로 2",
               "x":"127.1100","y":"35.8050","place_url":"http://place.map.kakao.com/1001","distance":"120"},
              {"id":"1002","place_name":"덕진24시동물병원","category_name":"가정,생활 > 반려동물 > 동물병원",
               "phone":"","address_name":"전북 전주시 덕진구 금암동","road_address_name":"",
               "x":"127.1200","y":"35.8400","place_url":"http://place.map.kakao.com/1002"}]}
            """;

    private final ExternalApiUsageService usageService = mock(ExternalApiUsageService.class);
    private final KakaoLocalProperties properties = new KakaoLocalProperties();
    private final Map<String, String> lastQuery = new LinkedHashMap<>();
    private final AtomicReference<String> lastAuthorization = new AtomicReference<>();

    private HttpServer server;
    private int status = 200;
    private long delayMs = 0;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/v2/local/search/keyword.json", exchange -> {
            lastAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            for (String pair : exchange.getRequestURI().getRawQuery().split("&")) {
                String[] kv = pair.split("=", 2);
                lastQuery.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] body = (status == 200 ? BODY : "{}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
            try {
                exchange.sendResponseHeaders(status, body.length);
                exchange.getResponseBody().write(body);
            } catch (IOException ignored) {
                /** 클라이언트가 시간 초과로 먼저 끊음 */
            } finally {
                exchange.close();
            }
        });
        server.start();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setRestApiKey("test-key");
        properties.setTimeoutMs(3000);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private KakaoPlaceSearchClient client() {
        return new KakaoPlaceSearchClient(properties, usageService);
    }

    private PlaceSearchQuery query(boolean distanceFirst) {
        return new PlaceSearchQuery("전주 동물병원", distanceFirst, new BigDecimal("35.70"), new BigDecimal("127.00"),
                new BigDecimal("35.95"), new BigDecimal("127.25"), new BigDecimal("35.8242"),
                new BigDecimal("127.1480"), 2);
    }

    private ExternalApiCallRequest recorded() {
        ArgumentCaptor<ExternalApiCallRequest> captor = ArgumentCaptor.forClass(ExternalApiCallRequest.class);
        verify(usageService).record(any(), captor.capture());
        return captor.getValue();
    }

    @Test
    void 요청_주소와_인증_헤더를_카카오_규격으로_보낸다() {
        client().search(query(false));

        assertThat(lastAuthorization.get()).isEqualTo("KakaoAK test-key");
        assertThat(lastQuery).containsEntry("query", "전주 동물병원")
                .containsEntry("rect", "127.00,35.70,127.25,35.95")
                .containsEntry("x", "127.1480")
                .containsEntry("y", "35.8242")
                .containsEntry("sort", "accuracy")
                .containsEntry("page", "2")
                .containsEntry("size", "15");
    }

    @Test
    void 좌표가_없으면_rect_x_y_없이_전국_정확도순으로_보낸다() {
        client().search(new PlaceSearchQuery("전주 동물병원", false, null, null, null, null, null, null, 1));

        assertThat(lastQuery).containsEntry("query", "전주 동물병원")
                .containsEntry("sort", "accuracy")
                .containsEntry("page", "1")
                .containsEntry("size", "15")
                .doesNotContainKeys("rect", "x", "y");
    }

    @Test
    void 거리순이면_sort를_distance로_보낸다() {
        client().search(query(true));

        assertThat(lastQuery).containsEntry("sort", "distance");
    }

    @Test
    void 응답을_장소_목록으로_바꾼다() {
        PlaceSearchPage page = client().search(query(false));

        assertThat(page.totalCount()).isEqualTo(31);
        assertThat(page.end()).isFalse();
        assertThat(page.items()).hasSize(2);
        PlaceSearchItem first = page.items().get(0);
        assertThat(first.kakaoPlcId()).isEqualTo("1001");
        assertThat(first.plcConm()).isEqualTo("온누리동물병원");
        assertThat(first.plcAddr()).isEqualTo("전북 전주시 완산구 효자로 2");
        assertThat(first.rprsTelno()).isEqualTo("063-245-0002");
        assertThat(first.kakaoUrlPath()).isEqualTo("http://place.map.kakao.com/1001");
        assertThat(first.plcLat()).isEqualByComparingTo("35.8050");
        assertThat(first.plcLot()).isEqualByComparingTo("127.1100");
        assertThat(first.categoryName()).isEqualTo("가정,생활 > 반려동물 > 동물병원");
        assertThat(page.items().get(1).plcAddr()).isEqualTo("전북 전주시 덕진구 금암동");
    }

    @Test
    void 성공하면_카카오_로컬_성공_로그를_남긴다() {
        client().search(query(false));

        ExternalApiCallRequest log = recorded();
        assertThat(log.extlApiSeCd()).isEqualTo("SYSC0018");
        assertThat(log.callRsltCd()).isEqualTo("SYSC0021");
        assertThat(log.httpSttsVl()).isEqualTo(200);
        assertThat(log.failRsn()).isNull();
    }

    @Test
    void 카카오가_오류를_주면_502와_실패_로그다() {
        status = 401;

        assertThatThrownBy(() -> client().search(query(false))).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXTERNAL_API_ERROR);

        ExternalApiCallRequest log = recorded();
        assertThat(log.callRsltCd()).isEqualTo("SYSC0022");
        assertThat(log.httpSttsVl()).isEqualTo(401);
        assertThat(log.failRsn()).isEqualTo("HTTP 401");
    }

    @Test
    void 제한_시간을_넘기면_502와_시간초과_로그다() {
        delayMs = 1500;
        properties.setTimeoutMs(300);

        assertThatThrownBy(() -> client().search(query(false))).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXTERNAL_API_ERROR);

        assertThat(recorded().callRsltCd()).isEqualTo("SYSC0023");
    }

    @Test
    void 키가_비어_있으면_호출하지_않고_502다() {
        properties.setRestApiKey("");

        assertThatThrownBy(() -> client().search(query(false))).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXTERNAL_API_ERROR);

        assertThat(lastAuthorization.get()).isNull();
        verify(usageService, never()).record(any(), any());
    }

    @Test
    void 로그_저장이_실패해도_검색_결과는_돌려준다() {
        when(usageService.record(any(), any())).thenThrow(new IllegalStateException("db down"));

        PlaceSearchPage page = client().search(query(false));

        assertThat(page.items()).hasSize(2);
    }
}
