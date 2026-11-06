package com.nyangmeong.admin.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.admin.dto.AdminDailyStatsSummary;
import com.nyangmeong.admin.dto.AdminDashboardStatsResponse;
import com.nyangmeong.admin.dto.AdminDashboardStatsSearchCond;
import com.nyangmeong.ai.dto.AdminDailyAiChatSummary;
import com.nyangmeong.ai.service.AiConversationService;
import com.nyangmeong.community.service.AdminPostService;
import com.nyangmeong.expense.dto.AdminDailyOcrRequestSummary;
import com.nyangmeong.expense.service.AdminReceiptService;
import com.nyangmeong.expense.service.ExpenseQueryService;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.member.dto.AdminDailyMemberSummary;
import com.nyangmeong.member.service.MemberService;
import com.nyangmeong.pet.service.PetService;
import com.nyangmeong.place.service.PlaceBookmarkService;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 대시보드 이용 통계 서비스]
 * - SCR-ADM-001 (FN-ADM-001-04·05) : 각 도메인 service의 건수를 합침
 * - 운영 현황·처리 대기(AdminDashboardService)와 의존성이 달라 나눔
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardStatsService {

    /** 조회할 수 있는 최대 일수 (양끝 포함) */
    static final int MAX_DAYS = 90;

    private final MemberService memberService;
    private final AiConversationService aiConversationService;
    private final AdminReceiptService adminReceiptService;
    private final AdminPostService adminPostService;
    private final ExpenseQueryService expenseQueryService;
    private final PetService petService;
    private final PlaceBookmarkService placeBookmarkService;

    /**
     * 이용 통계 조회
     */
    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse get(AdminDashboardStatsSearchCond cond) {
        validate(cond, LocalDate.now());
        LocalDate fromYmd = cond.getFromYmd();
        LocalDate toYmd = cond.getToYmd();
        LocalDateTime fromDt = fromYmd.atStartOfDay();
        LocalDateTime toDt = toYmd.plusDays(1).atStartOfDay();

        Map<LocalDate, AdminDailyMemberSummary> members = toMap(
                memberService.getDailyJoinWithdrawCounts(fromDt, toDt), AdminDailyMemberSummary::getYmd);
        Map<LocalDate, AdminDailyAiChatSummary> aiChats = toMap(
                aiConversationService.getDailyUserMessageCounts(fromDt, toDt), AdminDailyAiChatSummary::getYmd);
        Map<LocalDate, AdminDailyOcrRequestSummary> ocrRequests = toMap(
                adminReceiptService.getDailyRequestCounts(fromDt, toDt), AdminDailyOcrRequestSummary::getYmd);

        List<AdminDailyStatsSummary> daily = new ArrayList<>();
        for (LocalDate ymd = fromYmd; !ymd.isAfter(toYmd); ymd = ymd.plusDays(1)) {
            AdminDailyMemberSummary member = members.get(ymd);
            AdminDailyAiChatSummary aiChat = aiChats.get(ymd);
            AdminDailyOcrRequestSummary ocrRequest = ocrRequests.get(ymd);
            daily.add(new AdminDailyStatsSummary(ymd, member == null ? 0 : member.getJoinCnt(),
                    member == null ? 0 : member.getWhdwlCnt(), aiChat == null ? 0 : aiChat.getCnt(),
                    ocrRequest == null ? 0 : ocrRequest.getCnt()));
        }

        return new AdminDashboardStatsResponse(daily, adminPostService.getBoardPostCounts(fromDt, toDt),
                expenseQueryService.getCategoryCounts(fromYmd, toYmd), petService.getDistribution(),
                placeBookmarkService.getTopBookmarkedPlaces());
    }

    /**
     * 기간 검사 : 둘 다 필수, 시작 ≤ 종료 ≤ 오늘, 양끝 포함 최대 90일
     */
    void validate(AdminDashboardStatsSearchCond cond, LocalDate today) {
        LocalDate fromYmd = cond.getFromYmd();
        LocalDate toYmd = cond.getToYmd();
        if (fromYmd == null || toYmd == null || fromYmd.isAfter(toYmd) || toYmd.isAfter(today)
                || ChronoUnit.DAYS.between(fromYmd, toYmd) + 1 > MAX_DAYS) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private <T> Map<LocalDate, T> toMap(List<T> rows, Function<T, LocalDate> ymd) {
        return rows.stream().collect(Collectors.toMap(ymd, Function.identity()));
    }
}
