package com.nyangmeong.global.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * [검사 - 배치 스케줄 등록]
 */
class BatchScheduleRegistrarTest {

    @Test
    void 배치마다_cron을_등록하고_실행은_러너로_넘긴다() {
        BatchRunner runner = mock(BatchRunner.class);
        BatchJob job = mock(BatchJob.class);
        when(job.cron()).thenReturn("0 0 9 * * *");
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

        new BatchScheduleRegistrar(List.of(job), runner).configureTasks(registrar);

        CronTask task = registrar.getCronTaskList().getFirst();
        assertThat(task.getExpression()).isEqualTo("0 0 9 * * *");
        task.getRunnable().run();
        verify(runner).runScheduled(job);
    }
}
