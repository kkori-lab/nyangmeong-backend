package com.nyangmeong.expense.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.global.aitool.AiToolCatalogController;
import com.nyangmeong.global.aitool.AiToolCatalogService;
import com.nyangmeong.global.code.CommonCodeService;
import com.nyangmeong.global.code.CommonCodeSummary;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.crypto.TokenHashUtil;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;
import com.nyangmeong.expense.service.ExpenseQueryService;
import com.nyangmeong.expense.service.ExpenseService;
import com.nyangmeong.expense.service.ReceiptAnalyzer;
import com.nyangmeong.expense.service.ReceiptService;

/**
 * [검사 - 가계부 AI 삭제 도구]
 * - 지출 한 건 삭제가 DELETE에 경로 칸만으로 실리고, 여러 건 삭제는 쿼리 번호 목록으로 실리는지
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, ExpenseController.class, ReceiptController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class ExpenseDeleteAiToolCatalogTest {

    private static final String PATH = "/internal/ai/tools";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;
    @MockitoBean
    AuthMemberMapper authMemberMapper;
    @MockitoBean
    ExpenseService expenseService;
    @MockitoBean
    ExpenseQueryService expenseQueryService;
    @MockitoBean
    ReceiptAnalyzer receiptAnalyzer;
    @MockitoBean
    ReceiptService receiptService;

    private static String tool(String name) {
        return "$.data.tools[?(@.name == '" + name + "')]";
    }

    private void givenCodes() {
        CommonCodeSummary summary = new CommonCodeSummary();
        summary.setComCd("C0001");
        summary.setComCdNm("가");
        when(commonCodeService.search(anyString())).thenReturn(List.of(summary));
    }

    @Test
    void 지출_한_건_삭제_도구는_DELETE이고_경로_칸만_있다() throws Exception {
        givenCodes();
        String t = tool("delete_expense");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".kind").value("CARD"))
                .andExpect(jsonPath(t + ".domain").value("expense"))
                .andExpect(jsonPath(t + ".call.method").value("DELETE"))
                .andExpect(jsonPath(t + ".call.path").value("/api/expenses/{expndSn}"))
                .andExpect(jsonPath(t + ".params.expndSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.expndSn.required").value(true))
                .andExpect(jsonPath(t + ".params.*", hasSize(1)));
    }

    @Test
    void 지출_여러_건_삭제는_쿼리_번호_목록으로_싣는다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath("$.data.tools[?(@.call.path == '/api/expenses' && @.call.method == 'DELETE')].name",
                        contains("delete_expenses")));
    }
}
