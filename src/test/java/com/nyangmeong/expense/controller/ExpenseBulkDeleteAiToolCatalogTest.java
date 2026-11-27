package com.nyangmeong.expense.controller;

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
 * [검사 - 가계부 AI 여러 건·조건 전체 삭제 도구]
 * - 여러 건 삭제는 DELETE 쿼리 번호 목록, 조건 전체 삭제는 POST 본문 ExpenseBulkDeleteRequest
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, ExpenseController.class, ReceiptController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class ExpenseBulkDeleteAiToolCatalogTest {

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
    void 여러_건_삭제_도구는_DELETE이고_쿼리_번호_목록_칸만_있다() throws Exception {
        givenCodes();
        String t = tool("delete_expenses");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".kind").value("CARD"))
                .andExpect(jsonPath(t + ".domain").value("expense"))
                .andExpect(jsonPath(t + ".call.method").value("DELETE"))
                .andExpect(jsonPath(t + ".call.path").value("/api/expenses"))
                .andExpect(jsonPath(t + ".params.expndSn.in").value("query"))
                .andExpect(jsonPath(t + ".params.expndSn.type").value("array"))
                .andExpect(jsonPath(t + ".params.expndSn.itemType").value("long"))
                .andExpect(jsonPath(t + ".params.expndSn.required").value(true))
                .andExpect(jsonPath(t + ".params.*", hasSize(1)));
    }

    @Test
    void 조건_전체_삭제_도구는_POST_본문_칸을_싣는다() throws Exception {
        givenCodes();
        String t = tool("delete_expenses_by_condition");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".kind").value("CARD"))
                .andExpect(jsonPath(t + ".domain").value("expense"))
                .andExpect(jsonPath(t + ".call.method").value("POST"))
                .andExpect(jsonPath(t + ".call.path").value("/api/expenses/bulk-delete"))
                .andExpect(jsonPath(t + ".params.expectedCnt.in").value("body"))
                .andExpect(jsonPath(t + ".params.expectedCnt.required").value(true))
                .andExpect(jsonPath(t + ".params.expectedCnt.min").value(1))
                .andExpect(jsonPath(t + ".params.excludeExpndSn.type").value("array"))
                .andExpect(jsonPath(t + ".params.excludeExpndSn.itemType").value("long"))
                .andExpect(jsonPath(t + ".params.expndClsfCd.type").value("code"))
                .andExpect(jsonPath(t + ".params.expndClsfCd.group").value("MYPG05"))
                .andExpect(jsonPath(t + ".params.startYmd.type").value("date"))
                .andExpect(jsonPath(t + ".params.common.type").value("boolean"))
                .andExpect(jsonPath(t + ".params.*", hasSize(7)));
    }
}
