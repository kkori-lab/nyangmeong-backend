package com.nyangmeong.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.WithMockLoginMember;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;
import com.nyangmeong.notification.dto.AdminNotificationLogSearchCond;
import com.nyangmeong.notification.dto.AdminNotificationLogSummary;
import com.nyangmeong.notification.service.AdminNotificationLogService;

/**
 * [검사 - 관리자 알림 발송 이력 API]
 */
@WebMvcTest(controllers = AdminNotificationLogController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class AdminNotificationLogControllerTest {

    private static final String ADMIN = "USRC0005";
    private static final String PATH = "/api/admin/notification-logs";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AdminNotificationLogService adminNotificationLogService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember
    void 일반_회원은_403_ADMIN_ONLY() throws Exception {
        mockMvc.perform(get(PATH))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ONLY"));
        verify(adminNotificationLogService, never()).search(any(), any());
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 목록은_조건을_받아_페이지로_준다() throws Exception {
        AdminNotificationLogSummary row = new AdminNotificationLogSummary();
        row.setNotiSndngLogSn(11L);
        row.setMbrSn(4L);
        row.setMbrNcnm("나비집사");
        row.setNotiTypeCd("AIMC0008");
        row.setNotiChnlCd("AIMC0016");
        row.setSndngRsltCd("AIMC0019");
        row.setRtryNmtm(3);
        row.setFailRsn("SMTP 타임아웃");
        row.setSndngDt(LocalDateTime.of(2026, 10, 30, 9, 0));
        when(adminNotificationLogService.search(any(), any()))
                .thenReturn(new PageResponse<>(List.of(row), 1, 20, 1L, 1, false));

        mockMvc.perform(get(PATH).param("bgngYmd", "2026-10-29").param("endYmd", "2026-10-30")
                .param("notiChnlCd", "AIMC0016").param("sndngRsltCd", "AIMC0019").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].notiSndngLogSn").value(11))
                .andExpect(jsonPath("$.data.content[0].mbrNcnm").value("나비집사"))
                .andExpect(jsonPath("$.data.content[0].rtryNmtm").value(3))
                .andExpect(jsonPath("$.data.content[0].failRsn").value("SMTP 타임아웃"))
                .andExpect(jsonPath("$.data.content[0].notiCn").doesNotExist())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false));

        ArgumentCaptor<AdminNotificationLogSearchCond> cond = ArgumentCaptor
                .forClass(AdminNotificationLogSearchCond.class);
        ArgumentCaptor<PageQuery> pq = ArgumentCaptor.forClass(PageQuery.class);
        verify(adminNotificationLogService).search(cond.capture(), pq.capture());
        assertThat(cond.getValue().getBgngYmd()).isEqualTo(LocalDate.of(2026, 10, 29));
        assertThat(cond.getValue().getEndYmd()).isEqualTo(LocalDate.of(2026, 10, 30));
        assertThat(cond.getValue().getNotiChnlCd()).isEqualTo("AIMC0016");
        assertThat(cond.getValue().getSndngRsltCd()).isEqualTo("AIMC0019");
        assertThat(pq.getValue().page()).isEqualTo(1);
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 잘못된_조건은_400() throws Exception {
        when(adminNotificationLogService.search(any(), any()))
                .thenThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE));

        mockMvc.perform(get(PATH).param("notiChnlCd", "XXX"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 재발송_API는_없다() throws Exception {
        mockMvc.perform(post(PATH + "/11/resend"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(200));
        verify(adminNotificationLogService, never()).search(any(), any());
    }
}
