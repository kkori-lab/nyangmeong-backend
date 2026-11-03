package com.nyangmeong.global.code;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
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

/**
 * [검사 - 지역 목록 API]
 */
@WebMvcTest(controllers = RegionController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class RegionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RegionService regionService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private RegionSummary region(Long rgnSn, String sggNm) {
        RegionSummary region = new RegionSummary();
        region.setRgnSn(rgnSn);
        region.setCtpvNm("전북특별자치도");
        region.setSggNm(sggNm);
        return region;
    }

    @Test
    @WithMockLoginMember
    void 상위_지역을_주면_하위_지역을_돌려준다() throws Exception {
        when(regionService.getRegions(10L)).thenReturn(List.of(region(11L, "전주시"), region(12L, "군산시")));

        mockMvc.perform(get("/api/regions").param("upRgnSn", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sggNm").value("전주시"))
                .andExpect(jsonPath("$.data[1].rgnSn").value(12));
    }

    @Test
    @WithMockLoginMember
    void 상위_지역이_없으면_시도_목록() throws Exception {
        when(regionService.getRegions(null)).thenReturn(List.of(region(10L, "")));

        mockMvc.perform(get("/api/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].rgnSn").value(10));
    }
}
