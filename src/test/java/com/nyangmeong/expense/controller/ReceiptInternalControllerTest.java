package com.nyangmeong.expense.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.expense.domain.ReceiptOcrResult;
import com.nyangmeong.expense.dto.ReceiptReportResponse;
import com.nyangmeong.expense.service.ReceiptAnalyzer;
import com.nyangmeong.expense.service.ReceiptService;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.external.ReceiptOcrResponse;
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
 * [검사 - 영수증 원본 저장·결과 보고 내부 API] (FN-AIM-008-09·010-01·013-04·08)
 */
@WebMvcTest(controllers = ReceiptInternalController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class ReceiptInternalControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ReceiptService receiptService;

    @MockitoBean
    ReceiptAnalyzer receiptAnalyzer;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private final MockMultipartFile file = new MockMultipartFile("file", "r.png", "image/png", new byte[] {1});

    @Test
    @WithMockLoginMember
    void 원본_저장은_201과_영수증_번호를_돌려준다() throws Exception {
        when(receiptService.create(eq(1L), eq(10L), any(), eq("AIMC0031"))).thenReturn(3L);

        mockMvc.perform(multipart("/internal/ai/receipts").file(file).param("petSn", "10")
                .param("ocrDmndSrcCd", "AIMC0031").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recptSn").value(3));
    }

    @Test
    @WithMockLoginMember
    void 출처가_없거나_허용값_밖이면_400_INVALID_INPUT_VALUE() throws Exception {
        mockMvc.perform(multipart("/internal/ai/receipts").file(file).param("petSn", "10")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("ocrDmndSrcCd"));
        for (String code : List.of("AIMC0027", "chat")) {
            mockMvc.perform(multipart("/internal/ai/receipts").file(file).param("petSn", "10")
                    .param("ocrDmndSrcCd", code).header("X-Requested-With", "XMLHttpRequest"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
        }
        verify(receiptService, never()).create(any(), any(), any(), any());
    }

    @Test
    @WithMockLoginMember
    void 허용하지_않는_파일은_400_FILE_TYPE_NOT_ALLOWED() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("file", "r.pdf", "application/pdf", new byte[] {1});
        when(receiptService.create(eq(1L), eq(10L), any(), eq("AIMC0026")))
                .thenThrow(new CustomException(ErrorCode.FILE_TYPE_NOT_ALLOWED));

        mockMvc.perform(multipart("/internal/ai/receipts").file(pdf).param("petSn", "10")
                .param("ocrDmndSrcCd", "AIMC0026").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE_TYPE_NOT_ALLOWED"));
    }

    @Test
    @WithMockLoginMember
    void 결과_보고는_200과_반영한_상태를_돌려준다() throws Exception {
        when(receiptAnalyzer.report(eq(1L), eq(3L), eq(ReceiptOcrResult.RECOGNIZED), any()))
                .thenReturn(new ReceiptReportResponse(3L, "AIMC0002"));

        mockMvc.perform(post("/internal/ai/receipts/3/result").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ocrRslt":"RECOGNIZED","ocr":{"hsptlNm":"냥멍동물병원","ckupYmd":"2026-10-20",
                        "recptAmt":10000,"btmDscntAmt":null,"sections":[{"sectionNm":"미르","sbttlAmt":10000,
                        "dscntAmt":null,"items":[{"itemNm":"진찰료","itemAmt":10000}],"drugs":[]}]}}
                        """)
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recptSn").value(3))
                .andExpect(jsonPath("$.data.ocrPrcsSttsCd").value("AIMC0002"));
        ArgumentCaptor<ReceiptOcrResponse> ocr = ArgumentCaptor.forClass(ReceiptOcrResponse.class);
        verify(receiptAnalyzer).report(eq(1L), eq(3L), eq(ReceiptOcrResult.RECOGNIZED), ocr.capture());
        assertThat(ocr.getValue().sections()).singleElement()
                .satisfies(section -> assertThat(section.items()).hasSize(1));
    }

    @Test
    @WithMockLoginMember
    void 결과_없이_상태만_보고할_수_있다() throws Exception {
        when(receiptAnalyzer.report(1L, 3L, ReceiptOcrResult.TIMEOUT, null))
                .thenReturn(new ReceiptReportResponse(3L, "AIMC0005"));

        mockMvc.perform(post("/internal/ai/receipts/3/result").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ocrRslt\":\"TIMEOUT\"}").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ocrPrcsSttsCd").value("AIMC0005"));
        verify(receiptAnalyzer).report(eq(1L), eq(3L), eq(ReceiptOcrResult.TIMEOUT), isNull());
    }

    @Test
    @WithMockLoginMember
    void 결과_이름이_없으면_400_INVALID_INPUT_VALUE_허용값_밖이면_400_BAD_REQUEST() throws Exception {
        mockMvc.perform(post("/internal/ai/receipts/3/result").contentType(MediaType.APPLICATION_JSON)
                .content("{}").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data[0].field").value("ocrRslt"));
        mockMvc.perform(post("/internal/ai/receipts/3/result").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ocrRslt\":\"PARTIAL\"}").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verify(receiptAnalyzer, never()).report(any(), any(), any(), any());
    }

    @Test
    @WithMockLoginMember
    void 분석중이_아닌_영수증은_409() throws Exception {
        when(receiptAnalyzer.report(1L, 3L, ReceiptOcrResult.CANCELED, null))
                .thenThrow(new CustomException(ErrorCode.RECEIPT_ALREADY_CANCELED));

        mockMvc.perform(post("/internal/ai/receipts/3/result").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ocrRslt\":\"CANCELED\"}").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RECEIPT_ALREADY_CANCELED"));
    }

    @Test
    void 인증이_없으면_401() throws Exception {
        mockMvc.perform(post("/internal/ai/receipts/3/result").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ocrRslt\":\"CANCELED\"}").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(multipart("/internal/ai/receipts").file(file).param("petSn", "10")
                .param("ocrDmndSrcCd", "AIMC0031").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized());
    }
}
