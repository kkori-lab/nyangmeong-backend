package com.nyangmeong.global.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;

/**
 * [검사 - 관리자 배치 현황]
 */
class AdminBatchServiceTest {

    private static final String CARE_MISSION = BatchType.CARE_MISSION.getCode();
    private static final String ALARM = BatchType.CARE_SCHEDULE_ALARM.getCode();

    private final BatchExecutionMapper mapper = mock(BatchExecutionMapper.class);
    private final BatchRunner runner = mock(BatchRunner.class);
    private final AdminTaskLogService adminTaskLogService = mock(AdminTaskLogService.class);
    private final BatchJob job = mock(BatchJob.class);
    private final AdminBatchService service = new AdminBatchService(mapper, runner, adminTaskLogService,
            List.of(job));

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        when(job.type()).thenReturn(BatchType.CARE_MISSION);
        when(runner.lock(BatchType.CARE_MISSION)).thenReturn(true);
        doAnswer(invocation -> {
            invocation.<BatchExecution>getArgument(0).setBtchExcnHstrySn(1L);
            return 1;
        }).when(mapper).insert(any());
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private void complete(int status) {
        TransactionSynchronizationUtils.triggerAfterCompletion(status);
    }

    @Test
    void 목록은_모든_배치를_마지막_실행과_함께_돌려준다() {
        BatchExecution last = new BatchExecution();
        last.setBtchSeCd(CARE_MISSION);
        last.setExcnBgngDt(LocalDateTime.of(2026, 11, 1, 9, 0));
        last.setExcnRsltCd(BatchExecutionResult.SUCCESS.getCode());
        when(mapper.selectLatestList()).thenReturn(List.of(last));
        when(runner.isRunning(BatchType.CARE_MISSION)).thenReturn(true);

        List<BatchSummary> rows = service.search();

        assertThat(rows).extracting(BatchSummary::btchSeCd)
                .containsExactly("SYSC0006", "SYSC0007", "SYSC0008", "SYSC0009");
        assertThat(rows.get(0).lastRsltCd()).isEqualTo(BatchExecutionResult.SUCCESS.getCode());
        assertThat(rows.get(0).runningYn()).isTrue();
        assertThat(rows.get(0).runnableYn()).isTrue();
        assertThat(rows.get(1).lastBgngDt()).isNull();
        assertThat(rows.get(1).runnableYn()).isFalse();
    }

    @Test
    void 없는_코드의_이력은_404다() {
        assertThatThrownBy(() -> service.searchExecutions("SYSC9999", null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void 수동_실행은_이력과_감사_로그를_남기고_커밋_뒤에_시작한다() {
        BatchRunResponse response = service.run(9L, CARE_MISSION);

        assertThat(response.btchExcnHstrySn()).isEqualTo(1L);
        verify(adminTaskLogService).record(eq(9L), eq(AdminTaskType.PROCESS), eq("BTCH_EXCN_HSTRY"), eq(1L), any(),
                isNull(), isNull());
        verify(runner, never()).runManual(any(), any());

        complete(TransactionSynchronization.STATUS_COMMITTED);

        verify(runner).runManual(eq(job), any());
    }

    @Test
    void 트랜잭션이_롤백되면_실행하지_않고_잠금을_푼다() {
        service.run(9L, CARE_MISSION);

        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(runner, never()).runManual(any(), any());
        verify(runner).unlock(BatchType.CARE_MISSION);
    }

    @Test
    void 실행_중이면_409다() {
        when(runner.lock(BatchType.CARE_MISSION)).thenReturn(false);

        assertThatThrownBy(() -> service.run(9L, CARE_MISSION))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BATCH_ALREADY_RUNNING);
        verify(mapper, never()).insert(any());
    }

    @Test
    void 꺼진_배치는_409다() {
        assertThatThrownBy(() -> service.run(9L, ALARM))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BATCH_NOT_AVAILABLE);
    }

    @Test
    void 없는_코드의_수동_실행은_404다() {
        assertThatThrownBy(() -> service.run(9L, "SYSC9999"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
    }
}
