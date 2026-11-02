package com.nyangmeong.pet.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.nyangmeong.pet.domain.CharacterImagePart;
import com.nyangmeong.pet.service.PixelCharacterService;

/**
 * [검사 - 캐릭터 이미지 API]
 */
@WebMvcTest(controllers = PetCharacterController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class PetCharacterControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PixelCharacterService pixelCharacterService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember
    void 캐릭터_이미지는_PNG_파일_자체다() throws Exception {
        when(pixelCharacterService.getCharacterImage(1L, 10L, CharacterImagePart.FULL))
                .thenReturn(new StoredFile(new byte[] {9, 9}, "image/png", "character/default-cat.png"));

        mockMvc.perform(get("/api/pets/10/character-image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(new byte[] {9, 9}));
    }

    @Test
    @WithMockLoginMember
    void 얼굴만_요청할_수_있다() throws Exception {
        when(pixelCharacterService.getCharacterImage(1L, 10L, CharacterImagePart.FACE))
                .thenReturn(new StoredFile(new byte[] {1}, "image/png", "face"));

        mockMvc.perform(get("/api/pets/10/character-image").param("part", "face"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[] {1}));
    }

    @Test
    @WithMockLoginMember
    void 알_수_없는_범위는_400_INVALID_INPUT_VALUE() throws Exception {
        mockMvc.perform(get("/api/pets/10/character-image").param("part", "body"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @WithMockLoginMember
    void 양육하지_않는_반려동물은_403() throws Exception {
        when(pixelCharacterService.getCharacterImage(1L, 99L, CharacterImagePart.FULL))
                .thenThrow(new CustomException(ErrorCode.PET_ACCESS_DENIED));

        mockMvc.perform(get("/api/pets/99/character-image"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PET_ACCESS_DENIED"));
    }
}
