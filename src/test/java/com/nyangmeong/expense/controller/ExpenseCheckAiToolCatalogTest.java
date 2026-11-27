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
import com.nyangmeong.expense.service.ExpenseQueryService;
import com.nyangmeong.expense.service.ExpenseService;
import com.nyangmeong.expense.service.ReceiptSaveService;

/**
 * [검사 - 가계부 도메인 AI 확인 도구]
 * - 저장 전 확인 2개는 READ, 영수증 직접 입력 검진 등록은 CARD로 칸과 함께 실리는지
 */
@WebMvcTest(controllers = {AiToolCatalogController.class, ExpenseController.class, ReceiptSaveController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class, AiToolCatalogService.class, TokenHashUtil.class})
@TestPropertySource(properties = "app.security.permit-all-paths=/internal/ai/tools")
class ExpenseCheckAiToolCatalogTest {

    private static final String PATH = "/internal/ai/tools";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommonCodeService commonCodeService;
    @MockitoBean
    AuthMemberMapper authMemberMapper;
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
    void 가계부_중복_확인은_POST_READ로_지출_줄_목록을_준다() throws Exception {
        givenCodes();
        String t = tool("check_expense_duplicates");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".domain").value("expense"))
                .andExpect(jsonPath(t + ".kind").value("READ"))
                .andExpect(jsonPath(t + ".call.method").value("POST"))
                .andExpect(jsonPath(t + ".call.path").value("/api/expenses/duplicate-check"))
                .andExpect(jsonPath(t + ".params.lines.type").value("array"))
                .andExpect(jsonPath(t + ".params.lines.items.petSn.type").value("long"))
                .andExpect(jsonPath(t + ".params.lines.items.idmptDmndId.generated").value("uuid"));
    }

    @Test
    void 영수증_저장_전_확인은_POST_READ로_반려동물_탭을_준다() throws Exception {
        givenCodes();
        String t = tool("check_receipt_save");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".kind").value("READ"))
                .andExpect(jsonPath(t + ".call.method").value("POST"))
                .andExpect(jsonPath(t + ".call.path").value("/api/receipts/{recptSn}/save-check"))
                .andExpect(jsonPath(t + ".params.recptSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.ckupYmd.required").value(true))
                .andExpect(jsonPath(t + ".params.pets.type").value("array"))
                .andExpect(jsonPath(t + ".params.pets.minItems").value(1))
                .andExpect(jsonPath(t + ".params.pets.items.petSn.required").value(true))
                .andExpect(jsonPath(t + ".params.pets.items.amt.type").value("long"))
                .andExpect(jsonPath(t + ".params.pets.items.expenseYn.type").value("boolean"));
    }

    @Test
    void 영수증_직접_입력_검진_등록은_CARD로_자동_키와_코드_칸을_준다() throws Exception {
        givenCodes();
        String t = tool("draft_receipt_checkup");
        mockMvc.perform(get(PATH))
                .andExpect(jsonPath(t + ".kind").value("CARD"))
                .andExpect(jsonPath(t + ".call.method").value("POST"))
                .andExpect(jsonPath(t + ".call.path").value("/api/receipts/{recptSn}/checkups"))
                .andExpect(jsonPath(t + ".params.recptSn.in").value("path"))
                .andExpect(jsonPath(t + ".params.petSn.in").value("query"))
                .andExpect(jsonPath(t + ".params.petSn.required").value(true))
                .andExpect(jsonPath(t + ".params.idmptDmndId.generated").value("uuid"))
                .andExpect(jsonPath(t + ".params.idmptDmndId.required").doesNotExist())
                .andExpect(jsonPath(t + ".params.ckupTypeCd.type").value("code"))
                .andExpect(jsonPath(t + ".params.ckupTypeCd.group").value("HLTG01"))
                .andExpect(jsonPath(t + ".params.hsptlNm.maxLength").value(80));
    }
}
