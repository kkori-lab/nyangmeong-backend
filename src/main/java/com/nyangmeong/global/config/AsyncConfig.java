package com.nyangmeong.global.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.nyangmeong.global.batch.BatchType;

/**
 * [비동기 실행 설정]
 * - characterTaskExecutor : 픽셀 캐릭터 백그라운드 생성 (FN-MYP-010-09)
 * - batchTaskExecutor : 관리자 배치 수동 실행 (FN-ADM-111-03)
 */
@EnableAsync
@Configuration
public class AsyncConfig {

    public static final String CHARACTER_EXECUTOR = "characterTaskExecutor";
    public static final String BATCH_EXECUTOR = "batchTaskExecutor";

    /**
     * 캐릭터 생성 스레드 풀
     */
    @Bean(name = CHARACTER_EXECUTOR)
    public Executor characterTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("character-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    /**
     * 배치 수동 실행 스레드 풀 : 배치별 잠금이 있어 배치 종류 수를 넘지 않음
     */
    @Bean(name = BATCH_EXECUTOR)
    public Executor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(BatchType.values().length);
        executor.setMaxPoolSize(BatchType.values().length);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("batch-");
        executor.initialize();
        return executor;
    }
}
