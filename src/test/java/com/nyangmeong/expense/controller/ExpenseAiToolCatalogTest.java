package com.nyangmeong.expense.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
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
 * [검사 - 가계부 도메인 AI 도구 목록]
 * - 실제 가계부 컨트롤러의 @AiTool이 빠짐없이 실리는지 (READ 6)
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, ExpenseController.class, ReceiptController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class ExpenseAiToolCatalogTest {

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
    void 가계부_READ_6개가_빠짐없이_실린다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath("$.data.tools.length()").value(6))
                .andExpect(jsonPath("$.data.tools[*].name", containsInAnyOrder("get_expense_statistics",
                        "get_expense_total", "get_receipt", "list_expense_months", "list_expense_pets",
                        "list_expenses")))
                .andExpect(jsonPath("$.data.tools[?(@.kind == 'READ')].name", hasSize(6)))
                .andExpect(jsonPath("$.data.tools[?(@.domain == 'expense')].name", hasSize(6)));
    }

    @Test
    void 지출_목록_도구는_검색_조건과_페이지_칸이_있다() throws Exception {
        givenCodes();
        String t = tool("list_expenses");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.path").value("/api/expenses"))
                .andExpect(jsonPath(t + ".params.startYmd.type").value("date"))
                .andExpect(jsonPath(t + ".params.endYmd.in").value("query"))
                .andExpect(jsonPath(t + ".params.petSn.type").value("long"))
                .andExpect(jsonPath(t + ".params.common.type").value("boolean"))
                .andExpect(jsonPath(t + ".params.expndClsfCd.group").value("MYPG05"))
                .andExpect(jsonPath(t + ".params.expndClsfCd.codes[0].cd").value("C0001"))
                .andExpect(jsonPath(t + ".params.page.in").value("query"))
                .andExpect(jsonPath(t + ".params.size.in").value("query"));
    }

    @Test
    void 합계_도구는_검색_조건만_있다() throws Exception {
        givenCodes();
        String t = tool("get_expense_total");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.path").value("/api/expenses/totals"))
                .andExpect(jsonPath(t + ".params.expndClsfCd.group").value("MYPG05"))
                .andExpect(jsonPath(t + ".params.page").doesNotExist());
    }

    @Test
    void 월_통계_도구는_연월이_필수다() throws Exception {
        givenCodes();
        String t = tool("get_expense_statistics");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".params.yearMonth.in").value("query"))
                .andExpect(jsonPath(t + ".params.yearMonth.type").value("string"))
                .andExpect(jsonPath(t + ".params.yearMonth.required").value(true));
    }

    @Test
    void 영수증_도구는_번호가_경로이고_반려동물은_선택이다() throws Exception {
        givenCodes();
        String t = tool("get_receipt");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.path").value("/api/receipts/{recptSn}"))
                .andExpect(jsonPath(t + ".params.recptSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.petSn.in").value("query"))
                .andExpect(jsonPath(t + ".params.petSn.required").doesNotExist());
    }
}
