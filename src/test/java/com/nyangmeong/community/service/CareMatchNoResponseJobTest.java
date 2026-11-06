package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.community.domain.CareMatchStatus;
import com.nyangmeong.global.batch.BatchType;
import com.nyangmeong.global.setting.SystemSetting;
import com.nyangmeong.global.setting.SystemSettingService;

/**
 * [검사 - 펫돌봄 매칭 무응답 종료 배치]
 * - 수락 대기·확인 대기를 차례로 처리하고 처리 건수를 돌려주는지, 한 건 실패가 나머지를 막지 않는지
 */
class CareMatchNoResponseJobTest {

    private final CareMatchService careMatchService = mock(CareMatchService.class);
    private final SystemSettingService systemSettingService = mock(SystemSettingService.class);
    private final CareMatchNoResponseJob job = new CareMatchNoResponseJob(careMatchService, systemSettingService);

    private void stubDays(long days) {
        SystemSetting setting = new SystemSetting();
        setting.setMtchNoRspnsEndCrtrDayCnt(days);
        when(systemSettingService.get()).thenReturn(setting);
    }

    @Test
    void 배치_구분과_주기() {
        assertThat(job.type()).isEqualTo(BatchType.CARE_MATCH_NO_RESPONSE);
        assertThat(job.type().getCode()).isEqualTo("SYSC0031");
        assertThat(job.cron()).isEqualTo("0 0 3 * * *");
    }

    @Test
    void 수락_대기와_확인_대기를_설정_일수로_처리하고_건수를_돌려준다() {
        stubDays(10L);
        when(careMatchService.findNoResponseTargets(CareMatchStatus.REQUESTED, 10L)).thenReturn(List.of(1L, 2L));
        when(careMatchService.findNoResponseTargets(CareMatchStatus.AWAITING_CONFIRM, 10L)).thenReturn(List.of(3L));
        when(careMatchService.expireNoResponse(1L, CareMatchStatus.REQUESTED, 10L)).thenReturn(true);
        when(careMatchService.expireNoResponse(2L, CareMatchStatus.REQUESTED, 10L)).thenReturn(false);
        when(careMatchService.expireNoResponse(3L, CareMatchStatus.AWAITING_CONFIRM, 10L)).thenReturn(true);

        assertThat(job.execute()).isEqualTo(2);
    }

    @Test
    void 한_건이_실패해도_나머지를_처리한다() {
        stubDays(14L);
        when(careMatchService.findNoResponseTargets(CareMatchStatus.REQUESTED, 14L)).thenReturn(List.of(1L, 2L));
        when(careMatchService.findNoResponseTargets(CareMatchStatus.AWAITING_CONFIRM, 14L)).thenReturn(List.of());
        when(careMatchService.expireNoResponse(1L, CareMatchStatus.REQUESTED, 14L))
                .thenThrow(new IllegalStateException("잠금 시간 초과"));
        when(careMatchService.expireNoResponse(2L, CareMatchStatus.REQUESTED, 14L)).thenReturn(true);

        assertThat(job.execute()).isEqualTo(1);
        verify(careMatchService).expireNoResponse(2L, CareMatchStatus.REQUESTED, 14L);
    }
}
