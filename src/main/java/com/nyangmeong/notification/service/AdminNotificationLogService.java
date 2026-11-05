package com.nyangmeong.notification.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.notification.NotificationType;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.notification.domain.NotificationChannel;
import com.nyangmeong.notification.domain.SendingResult;
import com.nyangmeong.notification.dto.AdminNotificationLogSearchCond;
import com.nyangmeong.notification.dto.AdminNotificationLogSummary;
import com.nyangmeong.notification.mapper.NotificationSendingLogMapper;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 알림 발송 이력 서비스]
 * - SCR-ADM-091 (FN-ADM-091-01) 조회만 : 재발송은 없다 (FN-ADM-091-02, 자동 재시도만)
 */
@Service
@RequiredArgsConstructor
public class AdminNotificationLogService {

    private final NotificationSendingLogMapper notificationSendingLogMapper;

    /**
     * 발송 이력 목록 조회 (최신순)
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminNotificationLogSummary> search(AdminNotificationLogSearchCond cond, PageQuery pq) {
        if (cond.getBgngYmd() != null && cond.getEndYmd() != null && cond.getBgngYmd().isAfter(cond.getEndYmd())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (hasText(cond.getNotiChnlCd())
                && Arrays.stream(NotificationChannel.values()).noneMatch(c -> c.is(cond.getNotiChnlCd()))) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (hasText(cond.getNotiTypeCd())
                && Arrays.stream(NotificationType.values()).noneMatch(t -> t.is(cond.getNotiTypeCd()))) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (hasText(cond.getSndngRsltCd())
                && Arrays.stream(SendingResult.values()).noneMatch(r -> r.is(cond.getSndngRsltCd()))) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOrDefault());
        List<AdminNotificationLogSummary> rows = notificationSendingLogMapper.selectAdminList(cond);
        return PageResponse.of(new PageInfo<>(rows));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 최근 발송 실패 수 조회
     */
    @Transactional(readOnly = true)
    public long countFailedSince(LocalDateTime sinceDt) {
        return notificationSendingLogMapper.countByResultSince(SendingResult.FAIL.getCode(), sinceDt);
    }
}
