package com.nyangmeong.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.admin.dto.AdminDailyStatsSummary;
import com.nyangmeong.admin.dto.AdminDashboardStatsResponse;
import com.nyangmeong.admin.service.AdminDashboardStatsService;
import com.nyangmeong.community.dto.AdminBoardPostCountSummary;
import com.nyangmeong.expense.dto.AdminExpenseCategoryCountSummary;
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
import com.nyangmeong.pet.dto.AdminPetDistributionSummary;
import com.nyangmeong.place.dto.AdminFavoritePlaceSummary;

/**
 * [검사 - 관리자 대시보드 이용 통계 API]
 */
@WebMvcTest(controllers = AdminDashboardStatsController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class AdminDashboardStatsControllerTest {

    private static final String ADMIN = "USRC0005";
    private static final String USER = "USRC0004";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AdminDashboardStatsService adminDashboardStatsService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 관리자는_기간_이용_통계를_조회한다() throws Exception {
        AdminBoardPostCountSummary board = new AdminBoardPostCountSummary();
        board.setBbsCd("CMUC0002");
        board.setBbsNm("펫일상");
        board.setCnt(12);
        AdminExpenseCategoryCountSummary category = new AdminExpenseCategoryCountSummary();
        category.setCtgryCd("MYPC0012");
        category.setCtgryNm("병원");
        category.setCnt(4);
        AdminPetDistributionSummary pet = new AdminPetDistributionSummary();
        pet.setPetSeNm("강아지");
        pet.setVrtyNm("말티즈");
        pet.setCnt(30);
        AdminFavoritePlaceSummary place = new AdminFavoritePlaceSummary();
        place.setPlcSn(77L);
        place.setPlcNm("멍멍 카페");
        place.setCnt(9);
        when(adminDashboardStatsService.get(argThat(cond -> LocalDate.of(2026, 11, 5).equals(cond.getFromYmd())
                && LocalDate.of(2026, 11, 6).equals(cond.getToYmd()))))
                .thenReturn(new AdminDashboardStatsResponse(
                        List.of(new AdminDailyStatsSummary(LocalDate.of(2026, 11, 5), 3, 1, 20, 2),
                                new AdminDailyStatsSummary(LocalDate.of(2026, 11, 6), 0, 0, 0, 0)),
                        List.of(board), List.of(category), List.of(pet), List.of(place)));

        mockMvc.perform(get("/api/admin/dashboard/stats").param("fromYmd", "2026-11-05").param("toYmd", "2026-11-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.daily.length()").value(2))
                .andExpect(jsonPath("$.data.daily[0].ymd").value("2026-11-05"))
                .andExpect(jsonPath("$.data.daily[0].joinCnt").value(3))
                .andExpect(jsonPath("$.data.daily[0].whdwlCnt").value(1))
                .andExpect(jsonPath("$.data.daily[0].aiChatCnt").value(20))
                .andExpect(jsonPath("$.data.daily[0].ocrRqstCnt").value(2))
                .andExpect(jsonPath("$.data.daily[1].joinCnt").value(0))
                .andExpect(jsonPath("$.data.pstCntByBbs[0].bbsCd").value("CMUC0002"))
                .andExpect(jsonPath("$.data.pstCntByBbs[0].bbsNm").value("펫일상"))
                .andExpect(jsonPath("$.data.pstCntByBbs[0].cnt").value(12))
                .andExpect(jsonPath("$.data.expnsCntByCtgry[0].ctgryCd").value("MYPC0012"))
                .andExpect(jsonPath("$.data.expnsCntByCtgry[0].ctgryNm").value("병원"))
                .andExpect(jsonPath("$.data.expnsCntByCtgry[0].cnt").value(4))
                .andExpect(jsonPath("$.data.petDstrb[0].petSeNm").value("강아지"))
                .andExpect(jsonPath("$.data.petDstrb[0].vrtyNm").value("말티즈"))
                .andExpect(jsonPath("$.data.petDstrb[0].cnt").value(30))
                .andExpect(jsonPath("$.data.fvrtPlcs[0].plcSn").value(77))
                .andExpect(jsonPath("$.data.fvrtPlcs[0].plcNm").value("멍멍 카페"))
                .andExpect(jsonPath("$.data.fvrtPlcs[0].cnt").value(9));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 기간이_잘못되면_400이다() throws Exception {
        when(adminDashboardStatsService.get(any())).thenThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE));

        mockMvc.perform(get("/api/admin/dashboard/stats").param("fromYmd", "2026-08-01").param("toYmd", "2026-11-06"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @WithMockLoginMember(id = 9L, mbrAuthrtCd = ADMIN)
    void 날짜_형식이_틀리면_400이다() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats").param("fromYmd", "20261105").param("toYmd", "2026-11-06"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    @WithMockLoginMember(id = 2L, mbrAuthrtCd = USER)
    void 일반_회원은_403이다() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats").param("fromYmd", "2026-11-05").param("toYmd", "2026-11-06"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ONLY"));
    }
}
