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
import com.sun.net.httpserver.HttpServer;

/**
 * [검사 - 카카오 로컬 좌표 → 행정구역코드 변환]
 * - JDK 내장 HTTP 서버를 카카오 대신 띄워 요청 주소·헤더, 행정동 우선 선택, 오류 처리, 호출 로그를 확인
 */
class KakaoRegionResolverTest {

    private static final BigDecimal LAT = new BigDecimal("35.8242");
    private static final BigDecimal LOT = new BigDecimal("127.1480");

    private static final String BOTH = """
            {"meta":{"total_count":2},
             "documents":[
              {"region_type":"B","address_name":"전북특별자치도 전주시 완산구 중앙동","code":"5211110100"},
              {"region_type":"H","address_name":"전북특별자치도 전주시 완산구 중앙동","code":"5211151000"}]}
            """;

    private final ExternalApiUsageService usageService = mock(ExternalApiUsageService.class);
    private final KakaoLocalProperties properties = new KakaoLocalProperties();
    private final Map<String, String> lastQuery = new LinkedHashMap<>();
    private final AtomicReference<String> lastAuthorization = new AtomicReference<>();
    private final AtomicReference<String> lastPath = new AtomicReference<>();

    private HttpServer server;
    private int status = 200;
    private long delayMs = 0;
    private String body = BOTH;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/v2/local/geo/coord2regioncode.json", exchange -> {
            lastPath.set(exchange.getRequestURI().getPath());
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
            byte[] bytes = (status == 200 ? body : "{}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
            try {
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
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

    private KakaoRegionResolver resolver() {
        return new KakaoRegionResolver(properties, usageService);
    }

    private ExternalApiCallRequest recorded() {
        ArgumentCaptor<ExternalApiCallRequest> captor = ArgumentCaptor.forClass(ExternalApiCallRequest.class);
        verify(usageService).record(any(), captor.capture());
        return captor.getValue();
    }

    private void assertBadGateway() {
        assertThatThrownBy(() -> resolver().resolveAdmdstCd(LAT, LOT)).isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXTERNAL_API_ERROR);
    }

    @Test
    void 요청_주소와_인증_헤더를_카카오_규격으로_보낸다() {
        resolver().resolveAdmdstCd(LAT, LOT);

        assertThat(lastPath.get()).isEqualTo("/v2/local/geo/coord2regioncode.json");
        assertThat(lastAuthorization.get()).isEqualTo("KakaoAK test-key");
        assertThat(lastQuery).containsEntry("x", "127.1480").containsEntry("y", "35.8242");
    }

    @Test
    void 행정동과_법정동이_모두_있으면_행정동_코드를_쓴다() {
        assertThat(resolver().resolveAdmdstCd(LAT, LOT)).isEqualTo("5211151000");
    }

    @Test
    void 행정동이_없으면_법정동_코드를_쓴다() {
        body = """
                {"meta":{"total_count":1},
                 "documents":[{"region_type":"B","address_name":"전북특별자치도 완주군 삼례읍","code":"5271025000"}]}
                """;

        assertThat(resolver().resolveAdmdstCd(LAT, LOT)).isEqualTo("5271025000");
    }

    @Test
    void 결과가_비면_null이고_성공_로그를_남긴다() {
        body = "{\"meta\":{\"total_count\":0},\"documents\":[]}";

        assertThat(resolver().resolveAdmdstCd(LAT, LOT)).isNull();

        ExternalApiCallRequest log = recorded();
        assertThat(log.callRsltCd()).isEqualTo("SYSC0021");
    }

    @Test
    void 성공하면_카카오_로컬_성공_로그를_남긴다() {
        resolver().resolveAdmdstCd(LAT, LOT);

        ExternalApiCallRequest log = recorded();
        assertThat(log.extlApiSeCd()).isEqualTo("SYSC0018");
        assertThat(log.callRsltCd()).isEqualTo("SYSC0021");
        assertThat(log.httpSttsVl()).isEqualTo(200);
        assertThat(log.failRsn()).isNull();
    }

    @Test
    void 카카오가_오류를_주면_null이_아니라_502와_실패_로그다() {
        status = 401;

        assertBadGateway();

        ExternalApiCallRequest log = recorded();
        assertThat(log.callRsltCd()).isEqualTo("SYSC0022");
        assertThat(log.httpSttsVl()).isEqualTo(401);
        assertThat(log.failRsn()).isEqualTo("HTTP 401");
    }

    @Test
    void 제한_시간을_넘기면_502와_시간초과_로그다() {
        delayMs = 1500;
        properties.setTimeoutMs(300);

        assertBadGateway();

        assertThat(recorded().callRsltCd()).isEqualTo("SYSC0023");
    }

    @Test
    void 응답_형식이_다르면_502와_실패_로그다() {
        body = "{\"documents\":[{\"code\":\"5211151000\"}]}";

        assertBadGateway();

        ExternalApiCallRequest log = recorded();
        assertThat(log.callRsltCd()).isEqualTo("SYSC0022");
        assertThat(log.failRsn()).isEqualTo("응답 형식 오류");
    }

    @Test
    void 키가_비어_있으면_호출하지_않고_502다() {
        properties.setRestApiKey("");

        assertBadGateway();

        assertThat(lastAuthorization.get()).isNull();
        verify(usageService, never()).record(any(), any());
    }

    @Test
    void 로그_저장이_실패해도_변환_결과는_돌려준다() {
        when(usageService.record(any(), any())).thenThrow(new IllegalStateException("db down"));

        assertThat(resolver().resolveAdmdstCd(LAT, LOT)).isEqualTo("5211151000");
    }
}
