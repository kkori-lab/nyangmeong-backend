package com.nyangmeong.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.admin.dto.AdminDashboardResponse;
import com.nyangmeong.community.dto.AdminDashboardPostResponse;
import com.nyangmeong.community.service.AdminPostService;
import com.nyangmeong.community.service.AdminReportService;
import com.nyangmeong.expense.service.AdminReceiptService;
import com.nyangmeong.global.batch.AdminBatchService;
import com.nyangmeong.member.dto.AdminDashboardMemberSummary;
import com.nyangmeong.member.service.MemberService;
import com.nyangmeong.notification.service.AdminNotificationLogService;
import com.nyangmeong.pet.service.AdminCharacterJobService;
import com.nyangmeong.support.dto.AdminUnansweredInquiryCountResponse;
import com.nyangmeong.support.service.AdminInquiryService;

/**
 * [검사 - 관리자 대시보드]
 */
class AdminDashboardServiceTest {

    private final MemberService memberService = mock(MemberService.class);
    private final AdminPostService adminPostService = mock(AdminPostService.class);
    private final AdminReportService adminReportService = mock(AdminReportService.class);
    private final AdminInquiryService adminInquiryService = mock(AdminInquiryService.class);
    private final AdminCharacterJobService adminCharacterJobService = mock(AdminCharacterJobService.class);
    private final AdminReceiptService adminReceiptService = mock(AdminReceiptService.class);
    private final AdminNotificationLogService adminNotificationLogService = mock(AdminNotificationLogService.class);
    private final AdminBatchService adminBatchService = mock(AdminBatchService.class);
    private final AdminDashboardService service = new AdminDashboardService(memberService, adminPostService,
            adminReportService, adminInquiryService, adminCharacterJobService, adminReceiptService,
            adminNotificationLogService, adminBatchService);

    @Test
    void 도메인별_건수를_한_응답으로_합친다() {
        AdminDashboardMemberSummary member = new AdminDashboardMemberSummary();
        member.setTotMbrCnt(15);
        member.setNrmlMbrCnt(14);
        member.setStopMbrCnt(1);
        member.setTodayJoinCnt(2);
        member.setWeekJoinCnt(5);
        member.setRecentMbrs(List.of());
        when(memberService.getDashboardSummary()).thenReturn(member);
        when(adminPostService.getDashboardSummary()).thenReturn(new AdminDashboardPostResponse(30, List.of()));
        when(adminReportService.countReceived()).thenReturn(7L);
        when(adminInquiryService.countUnanswered()).thenReturn(new AdminUnansweredInquiryCountResponse(5, 2));
        when(adminCharacterJobService.countFailed()).thenReturn(1L);
        when(adminReceiptService.countOcrFailed()).thenReturn(3L);
        when(adminNotificationLogService.countFailedSince(any())).thenReturn(4L);
        when(adminBatchService.countLastFailed()).thenReturn(1L);

        AdminDashboardResponse res = service.get();

        assertThat(res.totMbrCnt()).isEqualTo(15);
        assertThat(res.nrmlMbrCnt()).isEqualTo(14);
        assertThat(res.stopMbrCnt()).isEqualTo(1);
        assertThat(res.todayJoinCnt()).isEqualTo(2);
        assertThat(res.weekJoinCnt()).isEqualTo(5);
        assertThat(res.totPstCnt()).isEqualTo(30);
        assertThat(res.dclrRcptCnt()).isEqualTo(7);
        assertThat(res.unansInqryCnt()).isEqualTo(5);
        assertThat(res.pixelInqryCnt()).isEqualTo(2);
        assertThat(res.charFailCnt()).isEqualTo(1);
        assertThat(res.ocrFailCnt()).isEqualTo(3);
        assertThat(res.notiFailCnt()).isEqualTo(4);
        assertThat(res.btchFailCnt()).isEqualTo(1);
    }

    @Test
    void 알림_발송_실패는_최근_24시간만_센다() {
        when(memberService.getDashboardSummary()).thenReturn(new AdminDashboardMemberSummary());
        when(adminPostService.getDashboardSummary()).thenReturn(new AdminDashboardPostResponse(0, List.of()));
        when(adminInquiryService.countUnanswered()).thenReturn(new AdminUnansweredInquiryCountResponse(0, 0));

        AdminDashboardResponse res = service.get();

        ArgumentCaptor<LocalDateTime> since = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(adminNotificationLogService).countFailedSince(since.capture());
        assertThat(since.getValue()).isCloseTo(LocalDateTime.now().minusHours(24), within(1, ChronoUnit.MINUTES));
        assertThat(res.recentMbrs()).isEmpty();
    }
}
