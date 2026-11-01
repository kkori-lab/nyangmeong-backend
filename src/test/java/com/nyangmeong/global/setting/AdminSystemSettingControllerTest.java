package com.nyangmeong.global.setting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.WithMockLoginMember;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;

/**
 * [검사 - 관리자 운영 설정 API]
 */
@WebMvcTest(controllers = AdminSystemSettingController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class AdminSystemSettingControllerTest {

    private static final String ADMIN = "USRC0005";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AdminSystemSettingService adminSystemSettingService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 관리자는_다섯_묶음으로_조회한다() throws Exception {
        when(adminSystemSettingService.get()).thenReturn(new SystemSetting());

        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberPet.petMaxCnt").value(10))
                .andExpect(jsonPath("$.data.community.lowstHrwgAplcnYr").value("2026"))
                .andExpect(jsonPath("$.data.notification.notiHideCrtrDayCnt").value(7))
                .andExpect(jsonPath("$.data.ai.aiCnvrsTurnCnt").value(6))
                .andExpect(jsonPath("$.data.file.fileMaxSz").value(10485760));
    }

    @Test
    @WithMockLoginMember
    void 일반_회원은_조회할_수_없다() throws Exception {
        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ONLY"));
    }

    @Test
    void 비로그인은_401() throws Exception {
        mockMvc.perform(get("/api/admin/settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 수정하면_바뀐_설정을_돌려준다() throws Exception {
        SystemSetting updated = new SystemSetting();
        updated.setPetMaxCnt(20L);
        when(adminSystemSettingService.update(any(), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/admin/settings").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content("{\"petMaxCnt\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberPet.petMaxCnt").value(20));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 범위_밖_값은_400_INVALID_INPUT_VALUE() throws Exception {
        mockMvc.perform(patch("/api/admin/settings").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content("{\"petMaxCnt\":101}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("petMaxCnt"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 최저시급_연도_형식_오류는_400() throws Exception {
        mockMvc.perform(patch("/api/admin/settings").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content("{\"lowstHrwgAplcnYr\":\"26\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("lowstHrwgAplcnYr"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 공백뿐인_문구는_400() throws Exception {
        mockMvc.perform(patch("/api/admin/settings").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content("{\"aiCmtryBscCn\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("aiCmtryBscCn"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 탈퇴_회원_보관일수를_보내도_값은_바뀌지_않는다() throws Exception {
        when(adminSystemSettingService.update(any(), any())).thenReturn(new SystemSetting());

        mockMvc.perform(patch("/api/admin/settings").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content("{\"whdwlMbrKpngDayCnt\":31}"))
                .andExpect(status().isOk());

        ArgumentCaptor<AdminSettingUpdateRequest> received = ArgumentCaptor.forClass(AdminSettingUpdateRequest.class);
        verify(adminSystemSettingService).update(any(), received.capture());

        SystemSettingMapper systemSettingMapper = mock(SystemSettingMapper.class);
        SystemSetting current = new SystemSetting();
        Long keepDayCnt = current.getWhdwlMbrKpngDayCnt();
        when(systemSettingMapper.selectCurrent()).thenReturn(current);
        new AdminSystemSettingService(new SystemSettingService(systemSettingMapper), systemSettingMapper,
                mock(AdminTaskLogService.class)).update(new LoginMember(9L, ADMIN), received.getValue());

        assertThat(current.getWhdwlMbrKpngDayCnt()).isEqualTo(keepDayCnt);
        verify(systemSettingMapper, never()).upsert(any(), any());
    }

    @Test
    @WithMockLoginMember
    void 일반_회원은_수정할_수_없다() throws Exception {
        mockMvc.perform(patch("/api/admin/settings").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content("{\"petMaxCnt\":20}"))
                .andExpect(status().isForbidden());
    }
}
