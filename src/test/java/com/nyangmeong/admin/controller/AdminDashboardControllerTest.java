package com.nyangmeong.admin.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.admin.dto.AdminDashboardResponse;
import com.nyangmeong.admin.service.AdminDashboardService;
import com.nyangmeong.community.dto.AdminRecentPostSummary;
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
import com.nyangmeong.member.dto.AdminRecentMemberSummary;

/**
 * [검사 - 관리자 대시보드 API]
 */
@WebMvcTest(controllers = AdminDashboardController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class AdminDashboardControllerTest {

    private static final String ADMIN = "USRC0005";
    private static final String USER = "USRC0004";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AdminDashboardService adminDashboardService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 관리자는_운영_현황과_처리_대기를_조회한다() throws Exception {
        AdminRecentMemberSummary member = new AdminRecentMemberSummary();
        member.setMbrSn(1290L);
        member.setMbrNcnm("초코맘");
        member.setRegDt(LocalDateTime.of(2026, 11, 5, 10, 0));
        AdminRecentPostSummary post = new AdminRecentPostSummary();
        post.setPstSn(5120L);
        post.setBbsCd("CMUC0002");
        post.setBbsNm("펫일상");
        post.setPstTtl("산책 다녀왔어요");
        when(adminDashboardService.get()).thenReturn(new AdminDashboardResponse(1284, 1270, 14, 12, 86, 3412,
                List.of(member), List.of(post), 7, 5, 2, 1, 3, 0, 1));

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totMbrCnt").value(1284))
                .andExpect(jsonPath("$.data.nrmlMbrCnt").value(1270))
                .andExpect(jsonPath("$.data.stopMbrCnt").value(14))
                .andExpect(jsonPath("$.data.todayJoinCnt").value(12))
                .andExpect(jsonPath("$.data.weekJoinCnt").value(86))
                .andExpect(jsonPath("$.data.totPstCnt").value(3412))
                .andExpect(jsonPath("$.data.recentMbrs[0].mbrSn").value(1290))
                .andExpect(jsonPath("$.data.recentMbrs[0].mbrNcnm").value("초코맘"))
                .andExpect(jsonPath("$.data.recentPsts[0].pstSn").value(5120))
                .andExpect(jsonPath("$.data.recentPsts[0].bbsNm").value("펫일상"))
                .andExpect(jsonPath("$.data.dclrRcptCnt").value(7))
                .andExpect(jsonPath("$.data.unansInqryCnt").value(5))
                .andExpect(jsonPath("$.data.pixelInqryCnt").value(2))
                .andExpect(jsonPath("$.data.charFailCnt").value(1))
                .andExpect(jsonPath("$.data.ocrFailCnt").value(3))
                .andExpect(jsonPath("$.data.notiFailCnt").value(0))
                .andExpect(jsonPath("$.data.btchFailCnt").value(1));
    }

    @Test
    @WithMockLoginMember(id = 2L, mbrAuthrtCd = USER)
    void 일반_회원은_403이다() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ONLY"));
    }

    @Test
    void 비로그인은_401이다() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
    }
}
