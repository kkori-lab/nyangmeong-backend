package com.nyangmeong.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

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
import com.nyangmeong.member.domain.SuspensionType;
import com.nyangmeong.member.dto.AdminMemberSummary;
import com.nyangmeong.member.dto.AdminSuspensionSummary;
import com.nyangmeong.member.dto.AdminWithdrawnMemberSummary;
import com.nyangmeong.member.service.MemberService;
import com.nyangmeong.member.service.MemberSuspensionService;

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
    MemberSuspensionService memberSuspensionService;

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

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 관리자는_정지_회원_목록을_조회한다() throws Exception {
        AdminSuspensionSummary row = new AdminSuspensionSummary();
        row.setMbrSn(1L);
        row.setSuspnRsn("욕설 반복");
        row.setPrcrNcnm("관리자01");
        when(memberSuspensionService.searchSuspensions(any(), any()))
                .thenReturn(new PageResponse<>(List.of(row), 1, 20, 1L, 1, false));

        mockMvc.perform(get("/api/admin/members/suspensions").param("q", "나비").param("type", "PERMANENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].suspnRsn").value("욕설 반복"))
                .andExpect(jsonPath("$.data.content[0].prcrNcnm").value("관리자01"));

        verify(memberSuspensionService).searchSuspensions(
                argThat(cond -> "나비".equals(cond.getQ()) && cond.getType() == SuspensionType.PERMANENT), any());
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 관리자는_탈퇴_회원_목록을_조회하고_암호문은_응답하지_않는다() throws Exception {
        AdminWithdrawnMemberSummary row = new AdminWithdrawnMemberSummary();
        row.setMbrSn(1L);
        row.setAcntEmlEnc("암호문");
        row.setMaskedAcntEml("lea***@gmail.com");
        row.setPurgeYmd(LocalDate.of(2026, 11, 24));
        when(memberService.searchWithdrawnForAdmin(any()))
                .thenReturn(new PageResponse<>(List.of(row), 1, 20, 1L, 1, false));

        mockMvc.perform(get("/api/admin/members/withdrawn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].maskedAcntEml").value("lea***@gmail.com"))
                .andExpect(jsonPath("$.data.content[0].purgeYmd").value("2026-11-24"))
                .andExpect(jsonPath("$.data.content[0].acntEmlEnc").doesNotExist());
    }

    @Test
    @WithMockLoginMember(id = 1L)
    void 일반_회원은_탈퇴_회원_목록을_조회할_수_없다() throws Exception {
        mockMvc.perform(get("/api/admin/members/withdrawn"))
                .andExpect(status().isForbidden());

        verify(memberService, never()).searchWithdrawnForAdmin(any());
    }

    @Test
    void 비로그인은_탈퇴_회원_목록에_401() throws Exception {
        mockMvc.perform(get("/api/admin/members/withdrawn"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 정지_종류를_비우면_전체다() throws Exception {
        mockMvc.perform(get("/api/admin/members/suspensions").param("type", ""))
                .andExpect(status().isOk());

        verify(memberSuspensionService).searchSuspensions(argThat(cond -> cond.getType() == null), any());
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 없는_정지_종류는_400() throws Exception {
        mockMvc.perform(get("/api/admin/members/suspensions").param("type", "WRONG"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockLoginMember
    void 일반_회원은_정지_회원_목록을_볼_수_없다() throws Exception {
        mockMvc.perform(get("/api/admin/members/suspensions"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ONLY"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 관리자는_회원을_정지한다() throws Exception {
        mockMvc.perform(post("/api/admin/members/1/suspension").contentType(MediaType.APPLICATION_JSON)
                .header("X-Requested-With", "XMLHttpRequest")
                .content("{\"suspnDayCnt\":7,\"suspnRsn\":\"욕설 반복\"}"))
                .andExpect(status().isOk());

        verify(memberSuspensionService).suspend(eq(9L), eq(1L),
                argThat(req -> req.suspnDayCnt() == 7 && "욕설 반복".equals(req.suspnRsn())));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 정지_사유가_없으면_400() throws Exception {
        mockMvc.perform(post("/api/admin/members/1/suspension").contentType(MediaType.APPLICATION_JSON)
                .header("X-Requested-With", "XMLHttpRequest")
                .content("{\"suspnDayCnt\":7,\"suspnRsn\":\" \"}"))
                .andExpect(status().isBadRequest());

        verify(memberSuspensionService, never()).suspend(any(), any(), any());
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 정지할_수_없는_회원은_409() throws Exception {
        doThrow(new CustomException(ErrorCode.MEMBER_NOT_SUSPENDABLE)).when(memberSuspensionService)
                .suspend(any(), any(), any());

        mockMvc.perform(post("/api/admin/members/1/suspension").contentType(MediaType.APPLICATION_JSON)
                .header("X-Requested-With", "XMLHttpRequest")
                .content("{\"suspnDayCnt\":null,\"suspnRsn\":\"광고 도배\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_SUSPENDABLE"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 관리자는_정지를_해제한다() throws Exception {
        mockMvc.perform(delete("/api/admin/members/1/suspension").contentType(MediaType.APPLICATION_JSON)
                .header("X-Requested-With", "XMLHttpRequest")
                .content("{\"rlsRsn\":\"이의 제기 인용\"}"))
                .andExpect(status().isOk());

        verify(memberSuspensionService).release(eq(9L), eq(1L), argThat(req -> "이의 제기 인용".equals(req.rlsRsn())));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 해제_사유가_없으면_400() throws Exception {
        mockMvc.perform(delete("/api/admin/members/1/suspension").contentType(MediaType.APPLICATION_JSON)
                .header("X-Requested-With", "XMLHttpRequest")
                .content("{}"))
                .andExpect(status().isBadRequest());

        verify(memberSuspensionService, never()).release(any(), any(), any());
    }
}
