package com.nyangmeong.global.batch;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.nyangmeong.global.config.AsyncConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [배치 실행과 이력 기록]
 * - 트랜잭션 밖에서 기록 -> 배치가 롤백돼도 이력은 남음
 * - 잠금은 메모리 : 서버가 죽어도 풀림, 서버 1대 전제
 * - 이력 기록 실패는 로그만 남김 : 이력 때문에 배치가 멈추면 안 됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchRunner {

    private static final int FAIL_RSN_MAX_LENGTH = 1000;
    private static final String INTERRUPTED_RSN = "서버 재시작으로 중단";

    private final BatchExecutionMapper batchExecutionMapper;

    private final Set<BatchType> runningTypes = ConcurrentHashMap.newKeySet();

    /**
     * 실행 잠금 획득
     */
    public boolean lock(BatchType type) {
        return runningTypes.add(type);
    }

    /**
     * 실행 잠금 해제
     */
    public void unlock(BatchType type) {
        runningTypes.remove(type);
    }

    /**
     * 실행 중 여부
     */
    public boolean isRunning(BatchType type) {
        return runningTypes.contains(type);
    }

    /**
     * 스케줄 실행
     */
    public void runScheduled(BatchJob job) {
        if (!lock(job.type())) {
            log.info("[Batch] 실행 중이라 건너뜀 type={}", job.type());
            return;
        }
        BatchExecution execution = BatchExecution.start(job.type(), BatchExecutionType.AUTO, null);
        try {
            batchExecutionMapper.insert(execution);
        } catch (RuntimeException e) {
            log.error("[Batch] 실행 이력 추가 실패 type={}", job.type(), e);
        }
        run(job, execution);
    }

    /**
     * 수동 실행 : 잠금과 실행 이력은 호출자가 먼저 만듦
     */
    @Async(AsyncConfig.BATCH_EXECUTOR)
    public void runManual(BatchJob job, BatchExecution execution) {
        run(job, execution);
    }

    /**
     * 서버 기동 시 끝나지 못한 실행 정리
     */
    @EventListener(ApplicationReadyEvent.class)
    public void closeInterrupted() {
        try {
            int closed = batchExecutionMapper.updateInterrupted(BatchExecutionResult.RUNNING.getCode(),
                    BatchExecutionResult.FAIL.getCode(), INTERRUPTED_RSN);
            log.info("[Batch] 끝나지 못한 실행 정리 : {}건", closed);
        } catch (RuntimeException e) {
            log.error("[Batch] 끝나지 못한 실행 정리 실패", e);
        }
    }

    private void run(BatchJob job, BatchExecution execution) {
        try {
            execution.setPrcsCnt(job.execute());
            execution.setExcnRsltCd(BatchExecutionResult.SUCCESS.getCode());
        } catch (RuntimeException e) {
            log.error("[Batch] 실행 실패 type={}", job.type(), e);
            execution.setExcnRsltCd(BatchExecutionResult.FAIL.getCode());
            execution.setFailRsn(failRsn(e));
        } finally {
            unlock(job.type());
        }
        finish(execution);
    }

    private void finish(BatchExecution execution) {
        if (execution.getBtchExcnHstrySn() == null) {
            return;
        }
        try {
            batchExecutionMapper.updateResult(execution);
        } catch (RuntimeException e) {
            log.error("[Batch] 실행 이력 수정 실패 btchExcnHstrySn={}", execution.getBtchExcnHstrySn(), e);
        }
    }

    private static String failRsn(RuntimeException e) {
        String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return message.length() > FAIL_RSN_MAX_LENGTH ? message.substring(0, FAIL_RSN_MAX_LENGTH) : message;
    }
}
