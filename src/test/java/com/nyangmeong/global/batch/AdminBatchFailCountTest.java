package com.nyangmeong.global.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.log.AdminTaskLogService;

/**
 * [검사 - 관리자 대시보드 실패 배치 수]
 */
class AdminBatchFailCountTest {

    private final BatchExecutionMapper mapper = mock(BatchExecutionMapper.class);
    private final AdminBatchService service = new AdminBatchService(mapper, mock(BatchRunner.class),
            mock(AdminTaskLogService.class), List.of());

    private BatchExecution latest(String btchSeCd, BatchExecutionResult result) {
        BatchExecution execution = new BatchExecution();
        execution.setBtchSeCd(btchSeCd);
        execution.setExcnRsltCd(result.getCode());
        return execution;
    }

    @Test
    void 마지막_실행이_실패한_배치만_센다() {
        when(mapper.selectLatestList()).thenReturn(List.of(
                latest(BatchType.CARE_MISSION.getCode(), BatchExecutionResult.FAIL),
                latest(BatchType.NEWS_COLLECT.getCode(), BatchExecutionResult.SUCCESS),
                latest(BatchType.MEMBER_PURGE.getCode(), BatchExecutionResult.RUNNING),
                latest(BatchType.CARE_SCHEDULE_ALARM.getCode(), BatchExecutionResult.FAIL)));

        assertThat(service.countLastFailed()).isEqualTo(2);
    }

    @Test
    void 코드에_없는_배치_이력은_세지_않는다() {
        when(mapper.selectLatestList()).thenReturn(List.of(latest("SYSC9999", BatchExecutionResult.FAIL)));

        assertThat(service.countLastFailed()).isZero();
    }
}
