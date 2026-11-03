package com.nyangmeong.place.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.WithMockLoginMember;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;
import com.nyangmeong.place.dto.PlaceBookmarkCreateRequest;
import com.nyangmeong.place.dto.PlaceBookmarkResponse;
import com.nyangmeong.place.dto.PlaceBookmarkSummary;
import com.nyangmeong.place.service.PlaceBookmarkService;

/**
 * [검사 - 장소 즐겨찾기 API]
 */
@WebMvcTest(controllers = PlaceBookmarkController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class PlaceBookmarkControllerTest {

    private static final String PATH = "/api/place-bookmarks";
    private static final String AJAX = "XMLHttpRequest";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PlaceBookmarkService placeBookmarkService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    void 쿠키가_없으면_401이다() throws Exception {
        mockMvc.perform(get(PATH).param("plcClsfCd", "PLCC0001")).andExpect(status().isUnauthorized());
        verify(placeBookmarkService, never()).search(any(), any());
    }

    @Test
    @WithMockLoginMember(id = 3L)
    void 저장하면_201과_번호를_준다() throws Exception {
        when(placeBookmarkService.create(eq(3L), any(PlaceBookmarkCreateRequest.class)))
                .thenReturn(new PlaceBookmarkResponse(21L, 7L));

        mockMvc.perform(post(PATH).header("X-Requested-With", AJAX).contentType(MediaType.APPLICATION_JSON)
                .content("{\"plcSn\":7}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.plcBkmkSn").value(21))
                .andExpect(jsonPath("$.data.plcSn").value(7));
    }

    @Test
    @WithMockLoginMember
    void 장소_번호가_빠지면_400이다() throws Exception {
        mockMvc.perform(post(PATH).header("X-Requested-With", AJAX).contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
        verify(placeBookmarkService, never()).create(any(), any());
    }

    @Test
    @WithMockLoginMember
    void 이미_즐겨찾기한_장소는_409다() throws Exception {
        when(placeBookmarkService.create(any(), any(PlaceBookmarkCreateRequest.class)))
                .thenThrow(new CustomException(ErrorCode.PLACE_BOOKMARK_ALREADY_EXISTS));

        mockMvc.perform(post(PATH).header("X-Requested-With", AJAX).contentType(MediaType.APPLICATION_JSON)
                .content("{\"plcSn\":7}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLACE_BOOKMARK_ALREADY_EXISTS"));
    }

    @Test
    @WithMockLoginMember(id = 3L)
    void 해제하면_200이고_data가_없다() throws Exception {
        mockMvc.perform(delete(PATH + "/21").header("X-Requested-With", AJAX))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(placeBookmarkService).delete(3L, 21L);
    }

    @Test
    @WithMockLoginMember
    void 없는_즐겨찾기_해제는_404다() throws Exception {
        org.mockito.Mockito.doThrow(new CustomException(ErrorCode.PLACE_BOOKMARK_NOT_FOUND))
                .when(placeBookmarkService).delete(any(), any());

        mockMvc.perform(delete(PATH + "/21").header("X-Requested-With", AJAX))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLACE_BOOKMARK_NOT_FOUND"));
    }

    @Test
    @WithMockLoginMember(id = 3L)
    void 목록은_카테고리로_조회하고_카카오맵_주소를_준다() throws Exception {
        PlaceBookmarkSummary row = new PlaceBookmarkSummary();
        row.setPlcBkmkSn(21L);
        row.setPlcSn(7L);
        row.setPlcClsfCd("PLCC0001");
        row.setPlcClsfCdNm("병원");
        row.setPlcConm("온누리동물병원");
        row.setPlcLat(new BigDecimal("35.8342"));
        row.setPlcLot(new BigDecimal("127.1480"));
        row.setKakaoUrlPath("http://place.map.kakao.com/1");
        when(placeBookmarkService.search(3L, "PLCC0001")).thenReturn(List.of(row));

        mockMvc.perform(get(PATH).param("plcClsfCd", "PLCC0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].plcBkmkSn").value(21))
                .andExpect(jsonPath("$.data[0].plcClsfCdNm").value("병원"))
                .andExpect(jsonPath("$.data[0].kakaoUrlPath").value("http://place.map.kakao.com/1"));
    }

    @Test
    @WithMockLoginMember
    void 목록에_카테고리가_빠지면_400이다() throws Exception {
        mockMvc.perform(get(PATH)).andExpect(status().isBadRequest());
        verify(placeBookmarkService, never()).search(any(), any());
    }
}
