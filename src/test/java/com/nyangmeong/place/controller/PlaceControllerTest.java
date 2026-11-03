package com.nyangmeong.place.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.response.PageQuery;
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
import com.nyangmeong.place.dto.PlaceSearchCond;
import com.nyangmeong.place.dto.PlaceSummary;
import com.nyangmeong.place.service.PlaceService;

/**
 * [검사 - 장소 검색 API]
 */
@WebMvcTest(controllers = PlaceController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class PlaceControllerTest {

    private static final String PATH = "/api/places";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PlaceService placeService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private MockHttpServletRequestBuilder search() {
        return search("keyword", "전주");
    }

    /** 기본 쿼리에서 한 값만 바꾼 요청 */
    private MockHttpServletRequestBuilder search(String name, String value) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("plcClsfCd", "PLCC0001");
        params.put("keyword", "전주");
        params.put("swLat", "35.70");
        params.put("swLot", "127.00");
        params.put("neLat", "35.95");
        params.put("neLot", "127.25");
        params.put("cntrLat", "35.8242");
        params.put("cntrLot", "127.1480");
        params.put(name, value);
        MockHttpServletRequestBuilder request = get(PATH);
        params.forEach(request::param);
        return request;
    }

    @Test
    void 쿠키가_없으면_401이다() throws Exception {
        mockMvc.perform(search()).andExpect(status().isUnauthorized());
        verify(placeService, never()).search(any(), any(), any());
    }

    @Test
    @WithMockLoginMember(id = 3L)
    void 검색_조건과_정렬_쪽을_바인딩하고_결과를_돌려준다() throws Exception {
        PlaceSummary row = new PlaceSummary();
        row.setPlcSn(11L);
        row.setPlcClsfCd("PLCC0001");
        row.setPlcClsfCdNm("병원");
        row.setPlcConm("온누리동물병원");
        row.setPlcLat(new BigDecimal("35.8342"));
        row.setPlcLot(new BigDecimal("127.1480"));
        row.setKakaoUrlPath("http://place.map.kakao.com/1");
        row.setDistance(1112);
        row.setPlcBkmkSn(5L);
        when(placeService.search(eq(3L), any(PlaceSearchCond.class), any(PageQuery.class)))
                .thenReturn(new PageResponse<>(List.of(row), 2, 15, 20, 2, false));

        mockMvc.perform(search().param("sort", "DISTANCE").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].plcSn").value(11))
                .andExpect(jsonPath("$.data.content[0].plcClsfCdNm").value("병원"))
                .andExpect(jsonPath("$.data.content[0].kakaoUrlPath").value("http://place.map.kakao.com/1"))
                .andExpect(jsonPath("$.data.content[0].distance").value(1112))
                .andExpect(jsonPath("$.data.content[0].plcBkmkSn").value(5))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(15));

        ArgumentCaptor<PlaceSearchCond> condCaptor = ArgumentCaptor.forClass(PlaceSearchCond.class);
        ArgumentCaptor<PageQuery> pageCaptor = ArgumentCaptor.forClass(PageQuery.class);
        verify(placeService).search(eq(3L), condCaptor.capture(), pageCaptor.capture());
        assertThat(condCaptor.getValue().getPlcClsfCd()).isEqualTo("PLCC0001");
        assertThat(condCaptor.getValue().getKeyword()).isEqualTo("전주");
        assertThat(condCaptor.getValue().getSwLat()).isEqualByComparingTo("35.70");
        assertThat(condCaptor.getValue().getCntrLot()).isEqualByComparingTo("127.1480");
        assertThat(pageCaptor.getValue().sort()).isEqualTo("DISTANCE");
        assertThat(pageCaptor.getValue().pageOrDefault()).isEqualTo(2);
    }

    @Test
    @WithMockLoginMember
    void 필수_쿼리가_빠지면_400이다() throws Exception {
        mockMvc.perform(get(PATH).param("plcClsfCd", "PLCC0001")).andExpect(status().isBadRequest());
        mockMvc.perform(search("plcClsfCd", "")).andExpect(status().isBadRequest());
        verify(placeService, never()).search(any(), any(), any());
    }

    @Test
    @WithMockLoginMember
    void 좌표가_범위를_벗어나면_400이다() throws Exception {
        mockMvc.perform(search("cntrLat", "95")).andExpect(status().isBadRequest());
        verify(placeService, never()).search(any(), any(), any());
    }

    @Test
    @WithMockLoginMember
    void 잘못된_정렬은_400이다() throws Exception {
        when(placeService.search(any(), any(PlaceSearchCond.class), any(PageQuery.class)))
                .thenThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE));

        mockMvc.perform(search().param("sort", "RANDOM")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockLoginMember
    void 카카오가_실패하면_502다() throws Exception {
        when(placeService.search(any(), any(PlaceSearchCond.class), any(PageQuery.class)))
                .thenThrow(new CustomException(ErrorCode.EXTERNAL_API_ERROR));

        mockMvc.perform(search()).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("EXTERNAL_API_ERROR"));
    }
}
