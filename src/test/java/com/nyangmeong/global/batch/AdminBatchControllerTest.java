package com.nyangmeong.global.batch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
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

/**
 * [검사 - 관리자 배치 현황 API]
 */
@WebMvcTest(controllers = AdminBatchController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class AdminBatchControllerTest {

    private static final String ADMIN = "USRC0005";
    private static final String USER = "USRC0001";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AdminBatchService adminBatchService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 관리자는_배치_목록을_조회한다() throws Exception {
        when(adminBatchService.search()).thenReturn(List.of(new BatchSummary("SYSC0006", "케어미션 출제", null, null,
                null, null, null, false, true)));

        mockMvc.perform(get("/api/admin/batches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].btchSeCd").value("SYSC0006"))
                .andExpect(jsonPath("$.data[0].runnableYn").value(true));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 관리자는_실행_이력을_조회한다() throws Exception {
        BatchExecutionSummary row = new BatchExecutionSummary();
        row.setBtchExcnHstrySn(1L);
        row.setMbrNcnm("관리자01");
        when(adminBatchService.searchExecutions(eq("SYSC0006"), any()))
                .thenReturn(new PageResponse<>(List.of(row), 1, 20, 1L, 1, false));

        mockMvc.perform(get("/api/admin/batches/SYSC0006/executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].mbrNcnm").value("관리자01"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 관리자는_수동_실행한다() throws Exception {
        when(adminBatchService.run(9L, "SYSC0006")).thenReturn(new BatchRunResponse(1L));

        mockMvc.perform(post("/api/admin/batches/SYSC0006/run").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.btchExcnHstrySn").value(1));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 실행_중이면_409다() throws Exception {
        when(adminBatchService.run(9L, "SYSC0006")).thenThrow(new CustomException(ErrorCode.BATCH_ALREADY_RUNNING));

        mockMvc.perform(post("/api/admin/batches/SYSC0006/run").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BATCH_ALREADY_RUNNING"));
    }

    @Test
    @WithMockLoginMember(id = 2L, mbrAuthrtCd = USER)
    void 일반_회원은_403이다() throws Exception {
        mockMvc.perform(get("/api/admin/batches")).andExpect(status().isForbidden());
    }

    @Test
    void 비로그인은_401이다() throws Exception {
        mockMvc.perform(get("/api/admin/batches")).andExpect(status().isUnauthorized());
    }
}
