package com.nyangmeong.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.github.pagehelper.PageHelper;
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

/**
 * [검사 - 관리자 알림 발송 이력 조회 조건]
 */
class AdminNotificationLogServiceTest {

    private final NotificationSendingLogMapper mapper = mock(NotificationSendingLogMapper.class);
    private final AdminNotificationLogService service = new AdminNotificationLogService(mapper);

    @AfterEach
    void clearPage() {
        PageHelper.clearPage();
    }

    private void assertInvalid(AdminNotificationLogSearchCond cond) {
        assertThatThrownBy(() -> service.search(cond, new PageQuery(null, null, null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        verify(mapper, never()).selectAdminList(any());
    }

    @Test
    void 조건이_없으면_그대로_조회한다() {
        AdminNotificationLogSummary row = new AdminNotificationLogSummary();
        row.setNotiSndngLogSn(7L);
        when(mapper.selectAdminList(any())).thenReturn(List.of(row));

        PageResponse<AdminNotificationLogSummary> page = service.search(new AdminNotificationLogSearchCond(),
                new PageQuery(null, null, null));

        assertThat(page.content()).extracting(AdminNotificationLogSummary::getNotiSndngLogSn).containsExactly(7L);
    }

    @Test
    void 기간과_채널_유형_결과_코드를_mapper에_넘긴다() {
        AdminNotificationLogSearchCond cond = new AdminNotificationLogSearchCond();
        cond.setBgngYmd(LocalDate.of(2026, 11, 2));
        cond.setEndYmd(LocalDate.of(2026, 11, 2));
        cond.setNotiChnlCd(NotificationChannel.EMAIL.getCode());
        cond.setNotiTypeCd(NotificationType.CARE_SCHEDULE.getCode());
        cond.setSndngRsltCd(SendingResult.FAIL.getCode());
        when(mapper.selectAdminList(any())).thenReturn(List.of());

        service.search(cond, new PageQuery(1, 20, null));

        ArgumentCaptor<AdminNotificationLogSearchCond> captor = ArgumentCaptor
                .forClass(AdminNotificationLogSearchCond.class);
        verify(mapper).selectAdminList(captor.capture());
        assertThat(captor.getValue()).isSameAs(cond);
    }

    @Test
    void 시작일이_종료일보다_늦으면_400() {
        AdminNotificationLogSearchCond cond = new AdminNotificationLogSearchCond();
        cond.setBgngYmd(LocalDate.of(2026, 11, 3));
        cond.setEndYmd(LocalDate.of(2026, 11, 2));
        assertInvalid(cond);
    }

    @Test
    void 모르는_채널_코드는_400() {
        AdminNotificationLogSearchCond cond = new AdminNotificationLogSearchCond();
        cond.setNotiChnlCd("XXX");
        assertInvalid(cond);
    }

    @Test
    void 모르는_유형_코드는_400() {
        AdminNotificationLogSearchCond cond = new AdminNotificationLogSearchCond();
        cond.setNotiTypeCd("AIMC9999");
        assertInvalid(cond);
    }

    @Test
    void 모르는_결과_코드는_400() {
        AdminNotificationLogSearchCond cond = new AdminNotificationLogSearchCond();
        cond.setSndngRsltCd(NotificationChannel.EMAIL.getCode());
        assertInvalid(cond);
    }
}
