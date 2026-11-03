package com.nyangmeong.community.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.community.dto.ChatRoomCreateRequest;
import com.nyangmeong.community.dto.ChatRoomResponse;
import com.nyangmeong.community.service.ChatRoomService;
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
 * [검사 - 펫돌봄 채팅하기 API]
 */
@WebMvcTest(controllers = ChatRoomController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class ChatRoomControllerTest {

    private static final String AJAX = "XMLHttpRequest";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ChatRoomService chatRoomService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private ChatRoomResponse response(boolean created) {
        return new ChatRoomResponse(11L, "CHTC0001", 7L, "산책 도와주실 분", 5L, "PCAC0001", 1L, "토리맘", 25L, "SITTER",
                null, null, created);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request(String body) {
        return post("/api/chat-rooms").contentType(MediaType.APPLICATION_JSON).content(body)
                .header("X-Requested-With", AJAX);
    }

    @Test
    @WithMockLoginMember
    void 새_방은_201() throws Exception {
        when(chatRoomService.create(eq(1L), any())).thenReturn(response(true));

        mockMvc.perform(request("{\"pstSn\":7}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.otoChatRoomSn").value(11))
                .andExpect(jsonPath("$.data.myRole").value("SITTER"));

        verify(chatRoomService).create(1L, new ChatRoomCreateRequest(7L));
    }

    @Test
    @WithMockLoginMember
    void 기존_방은_200() throws Exception {
        when(chatRoomService.create(eq(1L), any())).thenReturn(response(false));

        mockMvc.perform(request("{\"pstSn\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.otoChatRoomSn").value(11));
    }

    @Test
    @WithMockLoginMember
    void 게시글_번호가_없으면_400() throws Exception {
        mockMvc.perform(request("{}")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockLoginMember
    void 내_글이면_400() throws Exception {
        when(chatRoomService.create(eq(1L), any())).thenThrow(new CustomException(ErrorCode.CHAT_ROOM_OWN_POST));

        mockMvc.perform(request("{\"pstSn\":7}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAT_ROOM_OWN_POST"));
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mockMvc.perform(request("{\"pstSn\":7}")).andExpect(status().isUnauthorized());
    }
}
