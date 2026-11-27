package com.nyangmeong.notification.controller;

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
 * [검사 - 알림 도메인 AI 도구 목록 추가분]
 * - 사이트 기능 전체 표의 읽기 후보·알림 쓰기 카드에 붙인 @AiTool이 실리는지 (READ 3 + CARD 2)
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, NotificationController.class,
        NotificationSettingController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class NotificationAiToolCatalogTest {

    private static final String PATH = "/internal/ai/tools";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;
    @MockitoBean
    AuthMemberMapper authMemberMapper;
    @MockitoBean
    NotificationService notificationService;

    private static String tool(String name) {
        return "$.data.tools[?(@.name == '" + name + "')]";
    }

    private void givenCodes() {
        CommonCodeSummary summary = new CommonCodeSummary();
        summary.setComCd("C0001");
        summary.setComCdNm("가");
        when(commonCodeService.search(anyString())).thenReturn(List.of(summary));
    }

    @Test
    void 새_도구가_종류와_도메인과_함께_실린다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(tool("list_notifications") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("list_notifications") + ".domain").value("notification"))
                .andExpect(jsonPath(tool("get_unread_notification_count") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("get_unread_notification_count") + ".domain").value("notification"))
                .andExpect(jsonPath(tool("get_notification_settings") + ".kind").value("READ"))
                .andExpect(jsonPath(tool("get_notification_settings") + ".domain").value("notification"))
                .andExpect(jsonPath(tool("mark_all_notifications_read") + ".kind").value("CARD"))
                .andExpect(jsonPath(tool("mark_all_notifications_read") + ".domain").value("notification"))
                .andExpect(jsonPath(tool("mark_notification_read") + ".kind").value("CARD"))
                .andExpect(jsonPath(tool("mark_notification_read") + ".domain").value("notification"));
    }

    @Test
    void 알림_목록_도구는_탭_칸을_싣지_않는다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(tool("list_notifications") + ".params.tab").doesNotExist());
    }

    @Test
    void list_notifications_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("list_notifications");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/notifications"))
                .andExpect(jsonPath(t + ".params.page.in").value("query"))
                .andExpect(jsonPath(t + ".params.page.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.size.in").value("query"))
                .andExpect(jsonPath(t + ".params.size.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.*", hasSize(2)));
    }

    @Test
    void get_unread_notification_count_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("get_unread_notification_count");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/notifications/unread-count"))
                .andExpect(jsonPath(t + ".params.*", hasSize(0)));
    }

    @Test
    void get_notification_settings_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("get_notification_settings");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("GET"))
                .andExpect(jsonPath(t + ".call.path").value("/api/members/me/notification-settings"))
                .andExpect(jsonPath(t + ".params.*", hasSize(0)));
    }

    @Test
    void mark_all_notifications_read_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("mark_all_notifications_read");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("POST"))
                .andExpect(jsonPath(t + ".call.path").value("/api/notifications/read-all"))
                .andExpect(jsonPath(t + ".params.*", hasSize(0)));
    }

    @Test
    void mark_notification_read_호출_경로와_칸() throws Exception {
        givenCodes();
        String t = tool("mark_notification_read");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("POST"))
                .andExpect(jsonPath(t + ".call.path").value("/api/notifications/{inappNotiSn}/read"))
                .andExpect(jsonPath(t + ".params.inappNotiSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.inappNotiSn.required").value(true))
                .andExpect(jsonPath(t + ".params.*", hasSize(1)));
    }
}
