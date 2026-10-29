package com.nyangmeong.expense.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.expense.dto.ReceiptResponse;
import com.nyangmeong.expense.dto.ReceiptResultResponse;
import com.nyangmeong.expense.service.ReceiptAnalyzer;
import com.nyangmeong.expense.service.ReceiptService;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.external.ReceiptOcrResponse;
import com.nyangmeong.global.external.ReceiptOcrResponse.Line;
import com.nyangmeong.global.external.ReceiptOcrResponse.Section;
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
 * [검사 - AI 대화창 업로드 출처·분석 결과 다시 열기 API] (FN-AIM-013-04·06)
 */
@WebMvcTest(controllers = ReceiptController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class ReceiptResultControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ReceiptAnalyzer receiptAnalyzer;

    @MockitoBean
    ReceiptService receiptService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private final MockMultipartFile file = new MockMultipartFile("file", "r.png", "image/png", new byte[] {1});

    @Test
    @WithMockLoginMember
    void 업로드는_요청_출처를_넘긴다() throws Exception {
        when(receiptAnalyzer.create(eq(1L), eq(10L), any(), eq("AIMC0031")))
                .thenReturn(ReceiptResponse.empty(3L, "AIMC0003", null));

        mockMvc.perform(multipart("/api/receipts").file(file).param("petSn", "10").param("ocrDmndSrcCd", "AIMC0031")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recptSn").value(3));
        verify(receiptAnalyzer).create(eq(1L), eq(10L), any(), eq("AIMC0031"));
    }

    @Test
    @WithMockLoginMember
    void 허용하지_않는_출처는_400_INVALID_INPUT_VALUE() throws Exception {
        for (String code : List.of("AIMC0027", "AIMC0028", "chat")) {
            mockMvc.perform(multipart("/api/receipts").file(file).param("petSn", "10").param("ocrDmndSrcCd", code)
                    .header("X-Requested-With", "XMLHttpRequest"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                    .andExpect(jsonPath("$.data[0].field").value("ocrDmndSrcCd"));
        }
        verify(receiptAnalyzer, never()).create(any(), any(), any(), any());
    }

    @Test
    @WithMockLoginMember
    void 결과_다시_열기는_200과_저장된_OCR_응답을_돌려준다() throws Exception {
        ReceiptOcrResponse ocr = new ReceiptOcrResponse("냥멍동물병원", LocalDate.of(2026, 10, 21), 10_000L, null,
                List.of(new Section("미르", 10_000L, null, List.of(new Line("진찰료", 10_000L)), List.of())));
        when(receiptAnalyzer.getResult(1L, 3L, 10L))
                .thenReturn(new ReceiptResultResponse(3L, 10L, "AIMC0002", null, ocr));

        mockMvc.perform(get("/api/receipts/3").param("petSn", "10")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recptSn").value(3))
                .andExpect(jsonPath("$.data.petSn").value(10))
                .andExpect(jsonPath("$.data.ocrPrcsSttsCd").value("AIMC0002"))
                .andExpect(jsonPath("$.data.ocr.hsptlNm").value("냥멍동물병원"))
                .andExpect(jsonPath("$.data.ocr.ckupYmd").value("2026-10-21"))
                .andExpect(jsonPath("$.data.ocr.sections[0].sectionNm").value("미르"))
                .andExpect(jsonPath("$.data.ocr.sections[0].items[0].itemNm").value("진찰료"));
    }

    @Test
    @WithMockLoginMember
    void 남의_영수증은_404_RECEIPT_NOT_FOUND() throws Exception {
        when(receiptAnalyzer.getResult(1L, 3L, 10L)).thenThrow(new CustomException(ErrorCode.RECEIPT_NOT_FOUND));

        mockMvc.perform(get("/api/receipts/3").param("petSn", "10")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECEIPT_NOT_FOUND"));
    }

    @Test
    void 인증이_없으면_401() throws Exception {
        mockMvc.perform(get("/api/receipts/3").param("petSn", "10")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized());
    }
}
