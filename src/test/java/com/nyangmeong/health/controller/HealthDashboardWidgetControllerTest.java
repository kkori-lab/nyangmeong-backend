package com.nyangmeong.health.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.WithMockLoginMember;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;
import com.nyangmeong.health.dto.HealthDashboardWidgetResponse;
import com.nyangmeong.health.dto.HealthDashboardWidgetSummary;
import com.nyangmeong.health.service.HealthDashboardWidgetService;

/**
 * [검사 - 건강기록 대시보드 고른 항목 API]
 * - GET·PUT /api/members/me/dashboard-widgets 응답 모양과 빈 배열·빈 코드 400
 */
@WebMvcTest(controllers = HealthDashboardWidgetController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class HealthDashboardWidgetControllerTest {

    private static final String XHR = "X-Requested-With";
    private static final String URL = "/api/members/me/dashboard-widgets";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    HealthDashboardWidgetService healthDashboardWidgetService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private HealthDashboardWidgetResponse response() {
        HealthDashboardWidgetSummary widget = new HealthDashboardWidgetSummary();
        widget.setDshbdWdgtCd("HLTC0084");
        widget.setDshbdWdgtCdNm("체중");
        widget.setIndctYn(true);
        widget.setSortSeq(10);
        return new HealthDashboardWidgetResponse(false, List.of(widget));
    }

    @Test
    @WithMockLoginMember
    void 조회는_200과_savedYn_widgets만_준다() throws Exception {
        when(healthDashboardWidgetService.get(1L)).thenReturn(response());

        mockMvc.perform(get(URL).header(XHR, "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.savedYn").value(false))
                .andExpect(jsonPath("$.data.widgets[0].dshbdWdgtCd").value("HLTC0084"))
                .andExpect(jsonPath("$.data.widgets[0].indctYn").value(true))
                .andExpect(jsonPath("$.data.widgets[0].savedYn").doesNotExist());
    }

    @Test
    @WithMockLoginMember
    void 저장은_200() throws Exception {
        when(healthDashboardWidgetService.save(eq(1L), any())).thenReturn(response());

        mockMvc.perform(put(URL).header(XHR, "XMLHttpRequest").contentType(MediaType.APPLICATION_JSON)
                .content("{\"widgets\":[{\"dshbdWdgtCd\":\"HLTC0084\",\"indctYn\":true}]}"))
                .andExpect(status().isOk());

        verify(healthDashboardWidgetService).save(eq(1L), any());
    }

    @Test
    @WithMockLoginMember
    void 빈_배열은_400() throws Exception {
        mockMvc.perform(put(URL).header(XHR, "XMLHttpRequest").contentType(MediaType.APPLICATION_JSON)
                .content("{\"widgets\":[]}"))
                .andExpect(status().isBadRequest());

        verify(healthDashboardWidgetService, never()).save(any(), any());
    }

    @Test
    @WithMockLoginMember
    void 코드나_표시여부가_비면_400() throws Exception {
        mockMvc.perform(put(URL).header(XHR, "XMLHttpRequest").contentType(MediaType.APPLICATION_JSON)
                .content("{\"widgets\":[{\"dshbdWdgtCd\":\"\",\"indctYn\":true}]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(URL).header(XHR, "XMLHttpRequest").contentType(MediaType.APPLICATION_JSON)
                .content("{\"widgets\":[{\"dshbdWdgtCd\":\"HLTC0084\"}]}"))
                .andExpect(status().isBadRequest());

        verify(healthDashboardWidgetService, never()).save(any(), any());
    }
}
