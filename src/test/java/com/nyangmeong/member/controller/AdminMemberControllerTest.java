package com.nyangmeong.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
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
import com.nyangmeong.member.dto.AdminMemberSummary;
import com.nyangmeong.member.service.MemberService;

/**
 * [검사 - 관리자 회원 API]
 */
@WebMvcTest(controllers = AdminMemberController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class AdminMemberControllerTest {

    private static final String ADMIN = "USRC0005";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MemberService memberService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 관리자는_조건으로_회원_목록을_조회한다() throws Exception {
        AdminMemberSummary row = new AdminMemberSummary();
        row.setMbrSn(1L);
        row.setMbrNcnm("초코맘");
        row.setAcntEmlEnc("암호문");
        row.setMaskedAcntEml("cho***@gmail.com");
        when(memberService.searchForAdmin(any(), any()))
                .thenReturn(new PageResponse<>(List.of(row), 1, 20, 1L, 1, false));

        mockMvc.perform(get("/api/admin/members").param("q", "초코").param("status", "USRC0001")
                .param("joinPath", "USRC0008").param("role", "USRC0004").param("bgngYmd", "2026-10-01")
                .param("endYmd", "2026-10-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].maskedAcntEml").value("cho***@gmail.com"))
                .andExpect(jsonPath("$.data.content[0].acntEmlEnc").doesNotExist())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(memberService).searchForAdmin(argThat(cond -> "초코".equals(cond.getQ())
                && "USRC0001".equals(cond.getStatus()) && "USRC0008".equals(cond.getJoinPath())
                && "USRC0004".equals(cond.getRole()) && LocalDate.of(2026, 10, 1).equals(cond.getBgngYmd())
                && LocalDate.of(2026, 10, 30).equals(cond.getEndYmd())), any());
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 기간이_뒤집히면_400() throws Exception {
        when(memberService.searchForAdmin(any(), any()))
                .thenThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE));

        mockMvc.perform(get("/api/admin/members").param("bgngYmd", "2026-10-30").param("endYmd", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @WithMockLoginMember
    void 일반_회원은_조회할_수_없다() throws Exception {
        mockMvc.perform(get("/api/admin/members"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ONLY"));
    }

    @Test
    void 비로그인은_401() throws Exception {
        mockMvc.perform(get("/api/admin/members"))
                .andExpect(status().isUnauthorized());
    }
}
