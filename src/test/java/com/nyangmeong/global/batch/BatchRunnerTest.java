package com.nyangmeong.global.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * [검사 - 배치 실행과 이력 기록]
 */
class BatchRunnerTest {

    private final BatchExecutionMapper mapper = mock(BatchExecutionMapper.class);
    private final BatchRunner runner = new BatchRunner(mapper);
    private final BatchJob job = mock(BatchJob.class);

    BatchRunnerTest() {
        when(job.type()).thenReturn(BatchType.CARE_MISSION);
        doAnswer(invocation -> {
            invocation.<BatchExecution>getArgument(0).setBtchExcnHstrySn(1L);
            return 1;
        }).when(mapper).insert(any());
    }

    private BatchExecution savedResult() {
        ArgumentCaptor<BatchExecution> captor = ArgumentCaptor.forClass(BatchExecution.class);
        verify(mapper).updateResult(captor.capture());
        return captor.getValue();
    }

    @Test
    void 성공하면_처리_건수와_성공을_남기고_잠금을_푼다() {
        when(job.execute()).thenReturn(5);

        runner.runScheduled(job);

        BatchExecution saved = savedResult();
        assertThat(saved.getExcnSeCd()).isEqualTo(BatchExecutionType.AUTO.getCode());
        assertThat(saved.getExcnRsltCd()).isEqualTo(BatchExecutionResult.SUCCESS.getCode());
        assertThat(saved.getPrcsCnt()).isEqualTo(5);
        assertThat(runner.isRunning(BatchType.CARE_MISSION)).isFalse();
    }

    @Test
    void 실패하면_사유를_남기고_예외를_밖으로_내지_않는다() {
        when(job.execute()).thenThrow(new IllegalStateException("DB 끊김"));

        runner.runScheduled(job);

        BatchExecution saved = savedResult();
        assertThat(saved.getExcnRsltCd()).isEqualTo(BatchExecutionResult.FAIL.getCode());
        assertThat(saved.getFailRsn()).isEqualTo("DB 끊김");
        assertThat(runner.isRunning(BatchType.CARE_MISSION)).isFalse();
    }

    @Test
    void 실패_사유는_컬럼_길이로_자른다() {
        when(job.execute()).thenThrow(new IllegalStateException("가".repeat(1500)));

        runner.runScheduled(job);

        assertThat(savedResult().getFailRsn()).hasSize(1000);
    }

    @Test
    void 같은_배치가_실행_중이면_자동_실행은_건너뛴다() {
        runner.lock(BatchType.CARE_MISSION);

        runner.runScheduled(job);

        verify(job, never()).execute();
        verify(mapper, never()).insert(any());
        assertThat(runner.isRunning(BatchType.CARE_MISSION)).isTrue();
    }

    @Test
    void 이력을_못_남겨도_배치는_실행한다() {
        doThrow(new IllegalStateException("이력 실패")).when(mapper).insert(any());

        runner.runScheduled(job);

        verify(job).execute();
        verify(mapper, never()).updateResult(any());
        assertThat(runner.isRunning(BatchType.CARE_MISSION)).isFalse();
    }

    @Test
    void 결과를_못_남겨도_잠금을_푼다() {
        doThrow(new IllegalStateException("이력 실패")).when(mapper).updateResult(any());

        runner.runScheduled(job);

        assertThat(runner.isRunning(BatchType.CARE_MISSION)).isFalse();
    }

    @Test
    void 수동_실행은_호출자가_만든_이력에_결과를_남기고_잠금을_푼다() {
        runner.lock(BatchType.CARE_MISSION);
        BatchExecution execution = BatchExecution.start(BatchType.CARE_MISSION, BatchExecutionType.MANUAL, 9L);
        execution.setBtchExcnHstrySn(7L);

        runner.runManual(job, execution);

        assertThat(savedResult().getBtchExcnHstrySn()).isEqualTo(7L);
        verify(mapper, never()).insert(any());
        assertThat(runner.isRunning(BatchType.CARE_MISSION)).isFalse();
    }

    @Test
    void 기동하면_실행중_이력을_실패로_정리한다() {
        runner.closeInterrupted();

        verify(mapper).updateInterrupted(BatchExecutionResult.RUNNING.getCode(),
                BatchExecutionResult.FAIL.getCode(), "서버 재시작으로 중단");
    }

    @Test
    void 기동_정리가_실패해도_예외를_내지_않는다() {
        when(mapper.updateInterrupted(any(), any(), any())).thenThrow(new IllegalStateException("DB 끊김"));

        runner.closeInterrupted();
    }
}
