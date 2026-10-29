package com.nyangmeong.health.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import com.nyangmeong.health.dto.CareCompletionResponse;
import com.nyangmeong.health.dto.CareCompletionSummary;
import com.nyangmeong.health.dto.CareItemResponse;
import com.nyangmeong.health.dto.CareItemSummary;
import com.nyangmeong.health.service.CareItemService;

/**
 * [검사 - 토탈 케어리스트 API]
 */
@WebMvcTest(controllers = CareItemController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class CareItemControllerTest {

    private static final String XHR = "X-Requested-With";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CareItemService careItemService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private CareItemResponse itemResponse() {
        return new CareItemResponse(100L, "목욕 가기", "HLTC0037", "HLTC0039", "2주", null, "task_alt",
                LocalDate.of(2026, 11, 12), null, false, 0, false, null, null, null, null, null, 0);
    }

    private CareCompletionResponse completionResponse() {
        return new CareCompletionResponse(900L, 100L, LocalDate.of(2026, 10, 29), 1, LocalDate.of(2026, 11, 12),
                LocalDate.of(2026, 10, 29), 1);
    }

    @Test
    @WithMockLoginMember
    void 목록은_메인_표시와_이름_조건을_받아_항목_행을_돌려준다() throws Exception {
        CareItemSummary row = new CareItemSummary();
        row.setCareArtclSn(100L);
        row.setCareArtclNm("양치");
        row.setMainExpsrYn(true);
        row.setBscArtclYn(true);
        row.setTodayCmptnCnt(1);
        when(careItemService.search(eq(1L), eq(10L), any())).thenReturn(List.of(row));

        mockMvc.perform(get("/api/pets/10/care-items").param("mainExpsrYn", "true").param("careArtclNm", "양치"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].careArtclNm").value("양치"))
                .andExpect(jsonPath("$.data[0].mainExpsrYn").value(true))
                .andExpect(jsonPath("$.data[0].bscArtclYn").value(true))
                .andExpect(jsonPath("$.data[0].todayCmptnCnt").value(1));
        verify(careItemService).search(eq(1L), eq(10L),
                argThat(cond -> Boolean.TRUE.equals(cond.getMainExpsrYn()) && "양치".equals(cond.getCareArtclNm())));
    }

    @Test
    @WithMockLoginMember
    void 항목_추가는_201과_저장한_항목을_돌려준다() throws Exception {
        when(careItemService.create(eq(1L), eq(10L), any())).thenReturn(itemResponse());

        mockMvc.perform(post("/api/pets/10/care-items").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"careArtclNm":"목욕 가기","careZoneCd":"HLTC0037","careCycleCd":"HLTC0039",
                         "lastCmptnYmd":"2026-10-29"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.httpCode").value(201))
                .andExpect(jsonPath("$.data.careArtclSn").value(100))
                .andExpect(jsonPath("$.data.nxtmPrnmntYmd").value("2026-11-12"));
    }

    @Test
    @WithMockLoginMember
    void 항목명이_없거나_40자를_넘으면_400() throws Exception {
        mockMvc.perform(post("/api/pets/10/care-items").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"careZoneCd\":\"HLTC0036\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        mockMvc.perform(post("/api/pets/10/care-items").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"careArtclNm\":\"" + "가".repeat(41) + "\",\"careZoneCd\":\"HLTC0036\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        verify(careItemService, never()).create(anyLong(), anyLong(), any());
    }

    @Test
    @WithMockLoginMember
    void 직접입력_일수가_0이거나_네_자리를_넘으면_400() throws Exception {
        mockMvc.perform(post("/api/pets/10/care-items").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"careArtclNm\":\"a\",\"careZoneCd\":\"HLTC0037\",\"careCycleCd\":\"HLTC0042\","
                        + "\"cycleDrctInptDayCnt\":10000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @WithMockLoginMember
    void 항목_수정은_200() throws Exception {
        when(careItemService.update(eq(1L), eq(10L), eq(100L), any())).thenReturn(itemResponse());

        mockMvc.perform(patch("/api/pets/10/care-items/100").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"careCycleCd\":\"HLTC0039\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.careArtclSn").value(100));
    }

    @Test
    @WithMockLoginMember
    void 기본_항목_삭제는_400_CARE_ITEM_DEFAULT_NOT_DELETABLE() throws Exception {
        doThrow(new CustomException(ErrorCode.CARE_ITEM_DEFAULT_NOT_DELETABLE)).when(careItemService)
                .delete(1L, 10L, 100L);

        mockMvc.perform(delete("/api/pets/10/care-items/100").header(XHR, "XMLHttpRequest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CARE_ITEM_DEFAULT_NOT_DELETABLE"));
    }

    @Test
    @WithMockLoginMember
    void 항목_삭제는_200() throws Exception {
        mockMvc.perform(delete("/api/pets/10/care-items/100").header(XHR, "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
        verify(careItemService).delete(1L, 10L, 100L);
    }

    @Test
    @WithMockLoginMember
    void 메인_표시_저장은_항목_목록을_받아_저장한_항목을_돌려준다() throws Exception {
        CareItemSummary row = new CareItemSummary();
        row.setCareArtclSn(7L);
        row.setMainExpsrYn(true);
        row.setMainSortSeq(1);
        when(careItemService.save(eq(1L), eq(10L), any())).thenReturn(List.of(row));

        mockMvc.perform(put("/api/pets/10/care-item-exposures").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"careArtclSns\":[7]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].careArtclSn").value(7))
                .andExpect(jsonPath("$.data[0].mainSortSeq").value(1));
        verify(careItemService).save(eq(1L), eq(10L), argThat(req -> List.of(7L).equals(req.careArtclSns())));
    }

    @Test
    @WithMockLoginMember
    void 메인_표시_저장에_목록이_없으면_400이고_7개는_서비스가_400으로_막는다() throws Exception {
        mockMvc.perform(put("/api/pets/10/care-item-exposures").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));

        when(careItemService.save(eq(1L), eq(10L), any()))
                .thenThrow(new CustomException(ErrorCode.CARE_ITEM_MAIN_LIMIT_EXCEEDED));
        mockMvc.perform(put("/api/pets/10/care-item-exposures").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"careArtclSns\":[1,2,3,4,5,6,7]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CARE_ITEM_MAIN_LIMIT_EXCEEDED"));
    }

    @Test
    @WithMockLoginMember
    void 기록_내용_목록은_기간과_항목_조건을_받는다() throws Exception {
        CareCompletionSummary row = new CareCompletionSummary();
        row.setCareCmptnHstrySn(900L);
        row.setCareArtclNm("목욕");
        row.setRprsIconId("bathtub");
        row.setCmptnYmd(LocalDate.of(2026, 10, 29));
        when(careItemService.searchCompletions(eq(1L), eq(10L), any())).thenReturn(List.of(row));

        mockMvc.perform(get("/api/pets/10/care-completions")
                .param("careArtclSn", "100").param("fromYmd", "2026-10-01").param("toYmd", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].careCmptnHstrySn").value(900))
                .andExpect(jsonPath("$.data[0].rprsIconId").value("bathtub"));
        verify(careItemService).searchCompletions(eq(1L), eq(10L), argThat(cond -> Long.valueOf(100L)
                .equals(cond.getCareArtclSn()) && LocalDate.of(2026, 10, 1).equals(cond.getFromYmd())
                && LocalDate.of(2026, 10, 31).equals(cond.getToYmd())));
    }

    @Test
    @WithMockLoginMember
    void 처음_체크하면_201과_다음_예정일을_돌려준다() throws Exception {
        when(careItemService.complete(eq(1L), eq(10L), any()))
                .thenReturn(new CareItemService.Completed(completionResponse(), true));

        mockMvc.perform(post("/api/pets/10/care-completions").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"careArtclSn\":100}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.httpCode").value(201))
                .andExpect(jsonPath("$.data.careCmptnHstrySn").value(900))
                .andExpect(jsonPath("$.data.nxtmPrnmntYmd").value("2026-11-12"));
    }

    @Test
    @WithMockLoginMember
    void 같은_항목을_같은_날_다시_체크하면_200과_기존_기록을_돌려준다() throws Exception {
        when(careItemService.complete(eq(1L), eq(10L), any()))
                .thenReturn(new CareItemService.Completed(completionResponse(), false));

        mockMvc.perform(post("/api/pets/10/care-completions").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"careArtclSn\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.httpCode").value(200))
                .andExpect(jsonPath("$.data.careCmptnHstrySn").value(900));
    }

    @Test
    @WithMockLoginMember
    void 완료_체크에_항목이_없으면_400() throws Exception {
        mockMvc.perform(post("/api/pets/10/care-completions").header(XHR, "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @WithMockLoginMember
    void 완료_취소는_200이고_없는_기록은_404() throws Exception {
        mockMvc.perform(delete("/api/pets/10/care-completions/900").header(XHR, "XMLHttpRequest"))
                .andExpect(status().isOk());
        verify(careItemService).cancel(1L, 10L, 900L);

        doThrow(new CustomException(ErrorCode.CARE_COMPLETION_NOT_FOUND)).when(careItemService)
                .cancel(1L, 10L, 999L);
        mockMvc.perform(delete("/api/pets/10/care-completions/999").header(XHR, "XMLHttpRequest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CARE_COMPLETION_NOT_FOUND"));
    }
}
