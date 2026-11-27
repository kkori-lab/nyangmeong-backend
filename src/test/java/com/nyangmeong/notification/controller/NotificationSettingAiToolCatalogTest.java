package com.nyangmeong.notification.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.nyangmeong.global.aitool.AiToolCatalogController;
import com.nyangmeong.global.aitool.AiToolCatalogService;
import com.nyangmeong.global.code.CommonCodeService;
import com.nyangmeong.global.code.CommonCodeSummary;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.crypto.TokenHashUtil;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;
import com.nyangmeong.notification.service.NotificationService;

/**
 * [검사 - 알림 설정 저장 AI 도구]
 * - draft_notification_settings : PUT 본문 8칸 모두 필수, leadTimes는 enum 값 목록
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, NotificationController.class,
        NotificationSettingController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class NotificationSettingAiToolCatalogTest {

    private static final String PATH = "/internal/ai/tools";
    private static final String TOOL = "$.data.tools[?(@.name == 'draft_notification_settings')]";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;
    @MockitoBean
    AuthMemberMapper authMemberMapper;
    @MockitoBean
    NotificationService notificationService;

    private void givenCodes() {
        CommonCodeSummary summary = new CommonCodeSummary();
        summary.setComCd("C0001");
        summary.setComCdNm("가");
        when(commonCodeService.search(anyString())).thenReturn(List.of(summary));
    }

    @Test
    void 알림_설정_저장_카드가_호출_경로와_함께_실린다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(TOOL + ".kind").value("CARD"))
                .andExpect(jsonPath(TOOL + ".domain").value("notification"))
                .andExpect(jsonPath(TOOL + ".call.method").value("PUT"))
                .andExpect(jsonPath(TOOL + ".call.path").value("/api/members/me/notification-settings"))
                .andExpect(jsonPath(TOOL + ".params.*", hasSize(8)));
    }

    @Test
    void 켜고_끄는_칸은_모두_필수_boolean이다() throws Exception {
        givenCodes();
        ResultActions result = mockMvc.perform(get(PATH));
        for (String name : List.of("emlNotiRcptnYn", "inappNotiRcptnYn", "careSchdlNotiRcptnYn",
                "mtchDlngNotiRcptnYn", "cmntNotiRcptnYn", "lostExpryNotiRcptnYn", "expndLimNotiRcptnYn")) {
            result.andExpect(jsonPath(TOOL + ".params." + name + ".in").value("body"))
                    .andExpect(jsonPath(TOOL + ".params." + name + ".type").value("boolean"))
                    .andExpect(jsonPath(TOOL + ".params." + name + ".required").value(true));
        }
    }

    @Test
    void 사전_알림_시점은_enum_값_목록으로_싣는다() throws Exception {
        givenCodes();
        String p = TOOL + ".params.leadTimes";
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(p + ".in").value("body"))
                .andExpect(jsonPath(p + ".type").value("array"))
                .andExpect(jsonPath(p + ".required").value(true))
                .andExpect(jsonPath(p + ".itemType").value("string"))
                .andExpect(jsonPath(p + ".values[*]").value(contains("TODAY", "D1", "D3", "D7")))
                .andExpect(jsonPath(p + ".minItems").doesNotExist());
    }
}
