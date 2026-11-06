package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.batch.BatchType;

/**
 * [검사 - 실종 만료 사전 알림 배치]
 * - 7일 전·1일 전 대상을 차례로 처리하고 보낸 건수를 돌려주는지, 한 건 실패가 나머지를 막지 않는지
 */
class LostReportExpiryAlarmJobTest {

    private final LostReportService lostReportService = mock(LostReportService.class);
    private final LostReportExpiryAlarmJob job = new LostReportExpiryAlarmJob(lostReportService);

    @Test
    void 배치_구분과_주기() {
        assertThat(job.type()).isEqualTo(BatchType.LOST_EXPIRY_ALARM);
        assertThat(job.type().getCode()).isEqualTo("SYSC0032");
        assertThat(job.cron()).isEqualTo("0 0 9 * * *");
    }

    @Test
    void 칠일_전과_하루_전_대상에게_보내고_보낸_건수를_돌려준다() {
        when(lostReportService.findExpiryAlarmTargets(7)).thenReturn(List.of(1L, 2L));
        when(lostReportService.findExpiryAlarmTargets(1)).thenReturn(List.of(3L));
        when(lostReportService.sendExpiryAlarm(1L, 7)).thenReturn(true);
        when(lostReportService.sendExpiryAlarm(2L, 7)).thenReturn(false);
        when(lostReportService.sendExpiryAlarm(3L, 1)).thenReturn(true);

        assertThat(job.execute()).isEqualTo(2);
    }

    @Test
    void 한_건이_실패해도_나머지를_보낸다() {
        when(lostReportService.findExpiryAlarmTargets(7)).thenReturn(List.of(1L, 2L));
        when(lostReportService.findExpiryAlarmTargets(1)).thenReturn(List.of());
        when(lostReportService.sendExpiryAlarm(1L, 7)).thenThrow(new IllegalStateException("조회 실패"));
        when(lostReportService.sendExpiryAlarm(2L, 7)).thenReturn(true);

        assertThat(job.execute()).isEqualTo(1);
        verify(lostReportService).sendExpiryAlarm(2L, 7);
    }
}
