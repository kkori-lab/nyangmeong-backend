package com.nyangmeong.admin.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.admin.dto.AdminDashboardResponse;
import com.nyangmeong.community.service.AdminPostService;
import com.nyangmeong.community.service.AdminReportService;
import com.nyangmeong.expense.service.AdminReceiptService;
import com.nyangmeong.global.batch.AdminBatchService;
import com.nyangmeong.member.service.MemberService;
import com.nyangmeong.notification.service.AdminNotificationLogService;
import com.nyangmeong.pet.service.AdminCharacterJobService;
import com.nyangmeong.support.service.AdminInquiryService;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 대시보드 서비스]
 * - SCR-ADM-001 (FN-ADM-001-02·03) : 각 도메인 service의 건수를 합침
 * - 처리 대기 건수는 각 목록 화면과 같은 조건
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    /** 알림 발송 실패를 세는 기간(시간) */
    private static final int NOTI_FAIL_HOURS = 24;

    private final MemberService memberService;
    private final AdminPostService adminPostService;
    private final AdminReportService adminReportService;
    private final AdminInquiryService adminInquiryService;
    private final AdminCharacterJobService adminCharacterJobService;
    private final AdminReceiptService adminReceiptService;
    private final AdminNotificationLogService adminNotificationLogService;
    private final AdminBatchService adminBatchService;

    /**
     * 운영 현황·처리 대기 조회
     */
    @Transactional(readOnly = true)
    public AdminDashboardResponse get() {
        return AdminDashboardResponse.of(memberService.getDashboardSummary(), adminPostService.getDashboardSummary(),
                adminReportService.countReceived(), adminInquiryService.countUnanswered(),
                adminCharacterJobService.countFailed(), adminReceiptService.countOcrFailed(),
                adminNotificationLogService.countFailedSince(LocalDateTime.now().minusHours(NOTI_FAIL_HOURS)),
                adminBatchService.countLastFailed());
    }
}
