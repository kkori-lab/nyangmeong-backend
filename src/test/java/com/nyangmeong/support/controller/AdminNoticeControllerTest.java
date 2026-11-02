package com.nyangmeong.support.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
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
import com.nyangmeong.support.dto.AdminNoticeResponse;
import com.nyangmeong.support.dto.AdminNoticeTargetCountResponse;
import com.nyangmeong.support.service.AdminNoticeService;

/**
 * [검사 - 관리자 공지사항 API]
 */
@WebMvcTest(controllers = AdminNoticeController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class AdminNoticeControllerTest {

    private static final String ADMIN = "USRC0005";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AdminNoticeService adminNoticeService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private AdminNoticeResponse response() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 2, 10, 0);
        return new AdminNoticeResponse(3L, "점검 안내", "본문", true, true, 0, now, 12, now, now);
    }

    @Test
    @WithMockLoginMember
    void 일반_회원은_403_ADMIN_ONLY() throws Exception {
        mockMvc.perform(get("/api/admin/notices"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ONLY"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 알림_대상_수를_돌려준다() throws Exception {
        when(adminNoticeService.getNotificationTargetCount()).thenReturn(new AdminNoticeTargetCountResponse(1270));

        mockMvc.perform(get("/api/admin/notices/notification-target-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trgtCnt").value(1270));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 없는_공지_상세는_404() throws Exception {
        when(adminNoticeService.get(99L)).thenThrow(new CustomException(ErrorCode.NOTICE_NOT_FOUND));

        mockMvc.perform(get("/api/admin/notices/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTICE_NOT_FOUND"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 등록하면_201과_공지를_돌려준다() throws Exception {
        when(adminNoticeService.create(eq(9L), any())).thenReturn(response());

        mockMvc.perform(post("/api/admin/notices").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ntcMttrTtl\":\"점검 안내\",\"ntcMttrCn\":\"본문\",\"upendFixYn\":true,"
                        + "\"useYn\":true,\"notiSndngYn\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ntcMttrSn").value(3))
                .andExpect(jsonPath("$.data.notiTrgtCnt").value(12));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 제목이_200자를_넘으면_400() throws Exception {
        String title = "가".repeat(201);

        mockMvc.perform(post("/api/admin/notices").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ntcMttrTtl\":\"" + title + "\",\"ntcMttrCn\":\"본문\",\"upendFixYn\":false,"
                        + "\"useYn\":true,\"notiSndngYn\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("ntcMttrTtl"));
        verify(adminNoticeService, never()).create(any(), any());
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 알림_발송_여부가_없으면_400() throws Exception {
        mockMvc.perform(post("/api/admin/notices").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ntcMttrTtl\":\"제목\",\"ntcMttrCn\":\"본문\",\"upendFixYn\":false,\"useYn\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("notiSndngYn"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 수정에서_공백_제목은_400() throws Exception {
        mockMvc.perform(patch("/api/admin/notices/3").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content("{\"ntcMttrTtl\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("ntcMttrTtl"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 비노출로_수정한다() throws Exception {
        when(adminNoticeService.update(eq(9L), eq(3L), any())).thenReturn(response());

        mockMvc.perform(patch("/api/admin/notices/3").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content("{\"useYn\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ntcMttrSn").value(3));
    }
}
