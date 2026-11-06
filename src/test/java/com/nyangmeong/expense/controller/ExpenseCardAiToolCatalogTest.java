package com.nyangmeong.expense.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
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
import com.nyangmeong.expense.service.ExpenseLimitService;
import com.nyangmeong.expense.service.ExpenseQueryService;
import com.nyangmeong.expense.service.ExpenseService;
import com.nyangmeong.expense.service.ReceiptSaveService;

/**
 * [검사 - 가계부 도메인 AI 쓰기 도구]
 * - 지출 등록·수정·월 한도·영수증 저장이 CARD로 줄 목록·자동 키와 함께 실리는지
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, ExpenseController.class, ExpenseLimitController.class,
        ReceiptSaveController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class ExpenseCardAiToolCatalogTest {

    private static final String PATH = "/internal/ai/tools";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;
    @MockitoBean
    AuthMemberMapper authMemberMapper;
    @MockitoBean
    ExpenseLimitService expenseLimitService;
    @MockitoBean
    ExpenseQueryService expenseQueryService;
    @MockitoBean
    ExpenseService expenseService;
    @MockitoBean
    ReceiptSaveService receiptSaveService;

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
    void 가계부_쓰기_CARD_4개가_실린다() throws Exception {
        givenCodes();
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath("$.data.tools[?(@.kind == 'CARD')].name", containsInAnyOrder("draft_expense",
                        "draft_expense_limit", "draft_expense_update", "draft_receipt_save")));
    }

    @Test
    void 지출_등록_도구는_줄_목록과_자동_키를_준다() throws Exception {
        givenCodes();
        String t = tool("draft_expense");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".domain").value("expense"))
                .andExpect(jsonPath(t + ".call.method").value("POST"))
                .andExpect(jsonPath(t + ".call.path").value("/api/expenses"))
                .andExpect(jsonPath(t + ".params.expndYmd.required").value(true))
                .andExpect(jsonPath(t + ".params.expndClsfCd.group").value("MYPG05"))
                .andExpect(jsonPath(t + ".params.expndInptMthCd.group").value("MYPG06"))
                .andExpect(jsonPath(t + ".params.lines.type").value("array"))
                .andExpect(jsonPath(t + ".params.lines.minItems").value(1))
                .andExpect(jsonPath(t + ".params.lines.items.petSn.type").value("long"))
                .andExpect(jsonPath(t + ".params.lines.items.expndAmt.required").value(true))
                .andExpect(jsonPath(t + ".params.lines.items.expndAmt.min").value(1))
                .andExpect(jsonPath(t + ".params.lines.items.idmptDmndId.generated").value("uuid"))
                .andExpect(jsonPath(t + ".params.lines.items.idmptDmndId.required").doesNotExist());
    }

    @Test
    void 지출_수정_도구는_PATCH다() throws Exception {
        givenCodes();
        String t = tool("draft_expense_update");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("PATCH"))
                .andExpect(jsonPath(t + ".call.path").value("/api/expenses/{expndSn}"))
                .andExpect(jsonPath(t + ".params.expndSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.expndClsfCd.group").value("MYPG05"))
                .andExpect(jsonPath(t + ".params.common.type").value("boolean"))
                .andExpect(jsonPath(t + ".params.expndAmt.min").value(1));
    }

    @Test
    void 월_지출_한도_도구는_금액이_필수다() throws Exception {
        givenCodes();
        String t = tool("draft_expense_limit");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.method").value("PUT"))
                .andExpect(jsonPath(t + ".call.path").value("/api/members/me/expense-limit"))
                .andExpect(jsonPath(t + ".params.mmExpndLimAmt.required").value(true))
                .andExpect(jsonPath(t + ".params.mmExpndLimAmt.min").value(1));
    }

    @Test
    void 영수증_저장_도구는_반려동물_탭과_약품_줄을_중첩으로_준다() throws Exception {
        givenCodes();
        String t = tool("draft_receipt_save");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".call.path").value("/api/receipts/{recptSn}/save"))
                .andExpect(jsonPath(t + ".params.idmptDmndId.generated").value("uuid"))
                .andExpect(jsonPath(t + ".params.idmptDmndId.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.hsptlNm.maxLength").value(50))
                .andExpect(jsonPath(t + ".params.pets.minItems").value(1))
                .andExpect(jsonPath(t + ".params.pets.items.ckupTypeCd.group").value("HLTG01"))
                .andExpect(jsonPath(t + ".params.pets.items.ckupTypeCd.codes[0].cd").value("C0001"))
                .andExpect(jsonPath(t + ".params.pets.items.drugs.type").value("array"))
                .andExpect(jsonPath(t + ".params.pets.items.drugs.items.prscSttsCd.group").value("HLTG06"))
                .andExpect(jsonPath(t + ".params.pets.items.drugs.items.dayMdcnNmtm.positive").value(true));
    }
}
