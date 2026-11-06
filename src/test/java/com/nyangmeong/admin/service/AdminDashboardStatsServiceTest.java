package com.nyangmeong.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.admin.dto.AdminDailyStatsSummary;
import com.nyangmeong.admin.dto.AdminDashboardStatsResponse;
import com.nyangmeong.admin.dto.AdminDashboardStatsSearchCond;
import com.nyangmeong.ai.dto.AdminDailyAiChatSummary;
import com.nyangmeong.ai.service.AiConversationService;
import com.nyangmeong.community.dto.AdminBoardPostCountSummary;
import com.nyangmeong.community.service.AdminPostService;
import com.nyangmeong.expense.dto.AdminDailyOcrRequestSummary;
import com.nyangmeong.expense.dto.AdminExpenseCategoryCountSummary;
import com.nyangmeong.expense.service.AdminReceiptService;
import com.nyangmeong.expense.service.ExpenseQueryService;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.member.dto.AdminDailyMemberSummary;
import com.nyangmeong.member.service.MemberService;
import com.nyangmeong.pet.dto.AdminPetDistributionSummary;
import com.nyangmeong.pet.service.PetService;
import com.nyangmeong.place.dto.AdminFavoritePlaceSummary;
import com.nyangmeong.place.service.PlaceBookmarkService;

/**
 * [검사 - 관리자 대시보드 이용 통계 서비스]
 */
class AdminDashboardStatsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

    private final MemberService memberService = mock(MemberService.class);
    private final AiConversationService aiConversationService = mock(AiConversationService.class);
    private final AdminReceiptService adminReceiptService = mock(AdminReceiptService.class);
    private final AdminPostService adminPostService = mock(AdminPostService.class);
    private final ExpenseQueryService expenseQueryService = mock(ExpenseQueryService.class);
    private final PetService petService = mock(PetService.class);
    private final PlaceBookmarkService placeBookmarkService = mock(PlaceBookmarkService.class);
    private final AdminDashboardStatsService service = new AdminDashboardStatsService(memberService,
            aiConversationService, adminReceiptService, adminPostService, expenseQueryService, petService,
            placeBookmarkService);

    @Test
    void 기간_안_날짜를_빠짐없이_채우고_없는_날은_0이다() {
        LocalDate from = LocalDate.now().minusDays(2);
        LocalDate to = LocalDate.now();
        AdminDailyMemberSummary member = new AdminDailyMemberSummary();
        member.setYmd(from);
        member.setJoinCnt(3);
        member.setWhdwlCnt(1);
        AdminDailyAiChatSummary aiChat = new AdminDailyAiChatSummary();
        aiChat.setYmd(to);
        aiChat.setCnt(7);
        AdminDailyOcrRequestSummary ocr = new AdminDailyOcrRequestSummary();
        ocr.setYmd(from.plusDays(1));
        ocr.setCnt(2);
        AdminBoardPostCountSummary board = new AdminBoardPostCountSummary();
        board.setBbsCd("CMUC0001");
        AdminExpenseCategoryCountSummary category = new AdminExpenseCategoryCountSummary();
        category.setCtgryCd("MYPC0012");
        AdminPetDistributionSummary pet = new AdminPetDistributionSummary();
        pet.setVrtyNm("말티즈");
        AdminFavoritePlaceSummary place = new AdminFavoritePlaceSummary();
        place.setPlcSn(10L);
        when(memberService.getDailyJoinWithdrawCounts(from.atStartOfDay(), to.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(member));
        when(aiConversationService.getDailyUserMessageCounts(from.atStartOfDay(), to.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(aiChat));
        when(adminReceiptService.getDailyRequestCounts(from.atStartOfDay(), to.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(ocr));
        when(adminPostService.getBoardPostCounts(from.atStartOfDay(), to.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(board));
        when(expenseQueryService.getCategoryCounts(from, to)).thenReturn(List.of(category));
        when(petService.getDistribution()).thenReturn(List.of(pet));
        when(placeBookmarkService.getTopBookmarkedPlaces()).thenReturn(List.of(place));

        AdminDashboardStatsResponse res = service.get(cond(from, to));

        assertThat(res.daily()).containsExactly(new AdminDailyStatsSummary(from, 3, 1, 0, 0),
                new AdminDailyStatsSummary(from.plusDays(1), 0, 0, 0, 2), new AdminDailyStatsSummary(to, 0, 0, 7, 0));
        assertThat(res.pstCntByBbs()).containsExactly(board);
        assertThat(res.expnsCntByCtgry()).containsExactly(category);
        assertThat(res.petDstrb()).containsExactly(pet);
        assertThat(res.fvrtPlcs()).containsExactly(place);
        verify(expenseQueryService).getCategoryCounts(from, to);
    }

    @Test
    void 하루만_고르면_한_행이다() {
        LocalDate today = LocalDate.now();

        AdminDashboardStatsResponse res = service.get(cond(today, today));

        assertThat(res.daily()).containsExactly(new AdminDailyStatsSummary(today, 0, 0, 0, 0));
    }

    @Test
    void 양끝_포함_90일은_된다() {
        assertThatCode(() -> service.validate(cond(TODAY.minusDays(89), TODAY), TODAY)).doesNotThrowAnyException();
    }

    @Test
    void 양끝_포함_91일은_400이다() {
        assertInvalid(cond(TODAY.minusDays(90), TODAY));
    }

    @Test
    void 시작일이_종료일보다_뒤면_400이다() {
        assertInvalid(cond(TODAY, TODAY.minusDays(1)));
    }

    @Test
    void 종료일이_오늘보다_뒤면_400이다() {
        assertInvalid(cond(TODAY, TODAY.plusDays(1)));
    }

    @Test
    void 기간이_없으면_400이다() {
        assertInvalid(cond(null, TODAY));
        assertInvalid(cond(TODAY, null));
    }

    @Test
    void 기간이_잘못되면_집계하지_않는다() {
        LocalDate today = LocalDate.now();

        assertThatThrownBy(() -> service.get(cond(today, today.plusDays(1)))).isInstanceOf(CustomException.class);
        verifyNoInteractions(memberService, aiConversationService, adminReceiptService, adminPostService,
                expenseQueryService, petService, placeBookmarkService);
    }

    private void assertInvalid(AdminDashboardStatsSearchCond cond) {
        assertThatThrownBy(() -> service.validate(cond, TODAY)).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
    }

    private AdminDashboardStatsSearchCond cond(LocalDate from, LocalDate to) {
        AdminDashboardStatsSearchCond cond = new AdminDashboardStatsSearchCond();
        cond.setFromYmd(from);
        cond.setToYmd(to);
        return cond;
    }
}
