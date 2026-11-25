package com.nyangmeong.health.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import com.nyangmeong.health.dto.DailyHealthRecordResponse;
import com.nyangmeong.health.service.DailyHealthRecordService;

/**
 * [검사 - 일상 건강기록 일괄 등록 API]
 * - 201 배열 응답, 칸별 오류 형식 values[번호].칸, 칸 없는 오류는 메시지만 (※신규 정책)
 */
@WebMvcTest(controllers = DailyHealthRecordController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class DailyHealthRecordControllerTest {

    private static final String XHR = "X-Requested-With";
    private static final String URL = "/api/pets/10/daily-health-records/bulk";
    private static final String BODY = """
            {"values":[
              {"dailyRcdIdctCd":"HLTC0014","rcdDt":"2026-11-25T09:14:00","rcdNvl":4.6},
              {"dailyRcdIdctCd":"HLTC0017","rcdDt":"2026-11-25T07:50:00","crsdSttsCd":"HLTC0019"}]}
            """;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DailyHealthRecordService dailyHealthRecordService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember
    void 일괄_등록은_201과_건마다_단건_등록과_같은_모양을_돌려준다() throws Exception {
        when(dailyHealthRecordService.createAll(eq(1L), eq(10L), any())).thenReturn(List.of(
                new DailyHealthRecordResponse(501L, "HLTC0014", new BigDecimal("4.60"), null, null,
                        LocalDateTime.of(2026, 11, 25, 9, 14)),
                new DailyHealthRecordResponse(502L, "HLTC0017", null, "HLTC0019", "정상",
                        LocalDateTime.of(2026, 11, 25, 7, 50))));

        mockMvc.perform(post(URL).header(XHR, "XMLHttpRequest").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.httpCode").value(201))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].dailyHlthRcdSn").value(501))
                .andExpect(jsonPath("$.data[0].rcdNvl").value(4.6))
                .andExpect(jsonPath("$.data[1].crsdSttsCdNm").value("정상"));
        verify(dailyHealthRecordService).createAll(eq(1L), eq(10L), argThat(values -> values.size() == 2
                && "HLTC0017".equals(values.get(1).dailyRcdIdctCd())
                && "HLTC0019".equals(values.get(1).crsdSttsCd())));
    }

    @Test
    @WithMockLoginMember
    void 요청_형식_오류는_values_번호_칸_이름으로_400이고_서비스를_부르지_않는다() throws Exception {
        mockMvc.perform(post(URL).header(XHR, "XMLHttpRequest").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"values":[
                          {"dailyRcdIdctCd":"HLTC0014","rcdDt":"2026-11-25T09:14:00","rcdNvl":4.6},
                          {"dailyRcdIdctCd":"HLTC0015","rcdDt":"2026-11-25T08:00:00","rcdNvl":80.123}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("values[1].rcdNvl"));
        verify(dailyHealthRecordService, never()).createAll(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 서비스의_건_검사_오류는_values_번호_칸_이름으로_내려간다() throws Exception {
        when(dailyHealthRecordService.createAll(eq(1L), eq(10L), any()))
                .thenThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE, "values[1].crsdSttsCd"));

        mockMvc.perform(post(URL).header(XHR, "XMLHttpRequest").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("values[1].crsdSttsCd"))
                .andExpect(jsonPath("$.data[0].message").value(ErrorCode.INVALID_INPUT_VALUE.message()));
    }

    @Test
    @WithMockLoginMember
    void 칸에_붙일_수_없는_오류는_칸_이름_없이_메시지만_내려간다() throws Exception {
        when(dailyHealthRecordService.createAll(eq(1L), eq(10L), any()))
                .thenThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE));

        mockMvc.perform(post(URL).header(XHR, "XMLHttpRequest").contentType(MediaType.APPLICATION_JSON)
                .content("{\"values\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_INPUT_VALUE.message()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
