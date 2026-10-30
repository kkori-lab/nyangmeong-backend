package com.nyangmeong.pet.controller;

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
import com.nyangmeong.pet.dto.BreedSummary;
import com.nyangmeong.pet.service.BreedService;

/**
 * [검사 - 품종 목록 API]
 */
@WebMvcTest(controllers = BreedController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAccessDeniedHandler.class, RestAuthenticationEntryPoint.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class BreedControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    BreedService breedService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember
    void 구분별_품종을_정렬순서대로_돌려준다() throws Exception {
        when(breedService.getBreeds("PETC0001")).thenReturn(List.of(
                new BreedSummary(1L, "DOGV0001", "말티즈", "PETC0001", 10),
                new BreedSummary(2L, "DOGV0002", "푸들", "PETC0001", 20)));

        mockMvc.perform(get("/api/breeds").param("petSeCd", "PETC0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].vrtyNm").value("말티즈"))
                .andExpect(jsonPath("$.data[1].sortSeq").value(20));
    }

    @Test
    @WithMockLoginMember
    void 구분을_빼면_400() throws Exception {
        mockMvc.perform(get("/api/breeds")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockLoginMember
    void 알_수_없는_구분이면_400() throws Exception {
        when(breedService.getBreeds("PETC9999")).thenThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE));

        mockMvc.perform(get("/api/breeds").param("petSeCd", "PETC9999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }
}
