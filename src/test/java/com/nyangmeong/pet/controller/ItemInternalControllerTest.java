package com.nyangmeong.pet.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import com.nyangmeong.global.file.StoredFile;
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
import com.nyangmeong.pet.dto.BakeTargetPageResponse;
import com.nyangmeong.pet.service.ItemService;

/**
 * [검사 - 아이템 내부 API]
 */
@WebMvcTest(controllers = ItemInternalController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class ItemInternalControllerTest {

    private static final String URL = "/internal/ai/items/7/bake-targets";
    private static final String BAKE_IMAGE_URL = "/internal/ai/items/7/bake-image";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ItemService itemService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember(mbrAuthrtCd = "USRC0005")
    void 관리자_쿠키로_굽기_대상_페이지() throws Exception {
        when(itemService.searchBakeTargets(true, 7L, 2))
                .thenReturn(new BakeTargetPageResponse(List.of(3L, 4L), true));

        mockMvc.perform(get(URL).param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pixelCharSns[0]").value(3))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    @WithMockLoginMember
    void 일반_회원이면_403() throws Exception {
        when(itemService.searchBakeTargets(false, 7L, 0)).thenThrow(new CustomException(ErrorCode.ADMIN_ONLY));

        mockMvc.perform(get(URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ONLY"));
        verify(itemService).searchBakeTargets(false, 7L, 0);
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockLoginMember
    void 회원_쿠키로_굽기_참고_그림을_받는다() throws Exception {
        when(itemService.getBakeImage(7L)).thenReturn(new StoredFile(new byte[] {1, 2, 3}, "image/png", "hat.png"));

        mockMvc.perform(get(BAKE_IMAGE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }

    @Test
    @WithMockLoginMember(mbrAuthrtCd = "USRC0005")
    void 굽기_대상이_아닌_아이템의_그림은_404() throws Exception {
        when(itemService.getBakeImage(7L)).thenThrow(new CustomException(ErrorCode.ITEM_NOT_FOUND));

        mockMvc.perform(get(BAKE_IMAGE_URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ITEM_NOT_FOUND"));
    }

    @Test
    void 로그인하지_않으면_그림도_401() throws Exception {
        mockMvc.perform(get(BAKE_IMAGE_URL))
                .andExpect(status().isUnauthorized());
    }
}
