package com.nyangmeong.global.file;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
 * [검사 - 공통 파일 API]
 * - 로그인 회원 누구나 여는 파일만 연다 / 쿠키 없으면 401 / 도메인 API 전용이거나 없는 파일은 404
 * - 예전 주소 /api/files/public/{atchFileSn} 도 같은 동작
 */
@WebMvcTest(controllers = FileController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class FileControllerTest {

    private static final String PATH = "/api/files/{atchFileSn}";
    private static final String OLD_PATH = "/api/files/public/{atchFileSn}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FileStore fileStore;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember
    void 로그인하면_누구나_여는_파일을_연다() throws Exception {
        when(fileStore.loadShared(7L)).thenReturn(new StoredFile(new byte[] {1, 2, 3}, "image/png", "post.png"));

        mockMvc.perform(get(PATH, 7L))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=86400")))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }

    @Test
    @WithMockLoginMember
    void 예전_주소도_로그인하면_같은_파일을_연다() throws Exception {
        when(fileStore.loadShared(7L)).thenReturn(new StoredFile(new byte[] {1, 2, 3}, "image/png", "post.png"));

        mockMvc.perform(get(OLD_PATH, 7L))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }

    @Test
    void 쿠키_없이_열면_401이다() throws Exception {
        mockMvc.perform(get(PATH, 7L)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(OLD_PATH, 7L)).andExpect(status().isUnauthorized());
        verify(fileStore, never()).loadShared(7L);
    }

    @Test
    @WithMockLoginMember
    void 도메인_API_전용이거나_없는_파일은_404이다() throws Exception {
        when(fileStore.loadShared(8L)).thenThrow(new CustomException(ErrorCode.FILE_NOT_FOUND));

        mockMvc.perform(get(PATH, 8L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FILE_NOT_FOUND"));
        mockMvc.perform(get(OLD_PATH, 8L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FILE_NOT_FOUND"));
    }
}
