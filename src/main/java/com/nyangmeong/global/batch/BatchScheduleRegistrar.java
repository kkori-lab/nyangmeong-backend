package com.nyangmeong.global.batch;

import java.time.ZoneId;
import java.util.List;

import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * [배치 스케줄 등록]
 * - BatchJob 빈을 cron 으로 등록 -> 프록시 빈을 호출하므로 @Transactional 유지
 */
@Component
@RequiredArgsConstructor
public class BatchScheduleRegistrar implements SchedulingConfigurer {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final List<BatchJob> batchJobs;
    private final BatchRunner batchRunner;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        for (BatchJob job : batchJobs) {
            registrar.addCronTask(new CronTask(() -> batchRunner.runScheduled(job),
                    new CronTrigger(job.cron(), ZONE)));
        }
    }
}
