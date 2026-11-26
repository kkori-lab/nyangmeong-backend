package com.nyangmeong.global.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.batch.BatchType;

import tools.jackson.databind.json.JsonMapper;

/**
 * [검사 - 감사 로그 정리 (FN-ADM-114-04)]
 */
class AuditLogPurgeTest {

    private final AdminTaskLogMapper adminTaskLogMapper = mock(AdminTaskLogMapper.class);
    private final AdminTaskLogService service = new AdminTaskLogService(adminTaskLogMapper, JsonMapper.builder().build());

    @Test
    void 오늘에서_30일_뺀_날_0시보다_앞선_로그를_지운다() {
        assertThat(AdminTaskLogService.cutoff(LocalDate.of(2026, 12, 2))).isEqualTo(LocalDateTime.of(2026, 11, 2, 0, 0));
    }

    @Test
    void 지운_행_수를_돌려준다() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 10, 27, 0, 0);
        when(adminTaskLogMapper.deleteBefore(cutoff)).thenReturn(5);

        int deleted = service.purgeExpired(LocalDate.of(2026, 11, 26));

        assertThat(deleted).isEqualTo(5);
        verify(adminTaskLogMapper).deleteBefore(cutoff);
    }

    @Test
    void 배치는_매일_03시_30분에_감사_로그_정리로_돈다() {
        AuditLogPurgeJob job = new AuditLogPurgeJob(service);

        assertThat(job.type()).isEqualTo(BatchType.AUDIT_LOG_PURGE);
        assertThat(job.type().getCode()).isEqualTo("SYSC0034");
        assertThat(job.cron()).isEqualTo("0 30 3 * * *");
    }
}
