package com.nyangmeong.global.batch;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 배치 현황]
 * - SCR-ADM-111 (FN-ADM-111-01·02·03)
 * - 수동 실행은 커밋 뒤 비동기 시작 : 실행 이력 행이 먼저 보여야 결과를 쓸 수 있음
 */
@Service
@RequiredArgsConstructor
public class AdminBatchService {

    private static final String TABLE_NAME = "BTCH_EXCN_HSTRY";

    private final BatchExecutionMapper batchExecutionMapper;
    private final BatchRunner batchRunner;
    private final AdminTaskLogService adminTaskLogService;
    private final List<BatchJob> batchJobs;

    /**
     * 배치 목록 조회
     */
    @Transactional(readOnly = true)
    public List<BatchSummary> search() {
        Map<String, BatchExecution> latest = batchExecutionMapper.selectLatestList().stream()
                .collect(Collectors.toMap(BatchExecution::getBtchSeCd, Function.identity()));
        return Arrays.stream(BatchType.values())
                .map(type -> BatchSummary.from(type, latest.get(type.getCode()), batchRunner.isRunning(type),
                        hasJob(type)))
                .toList();
    }

    /**
     * 배치 실행 이력 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<BatchExecutionSummary> searchExecutions(String btchSeCd, PageQuery pq) {
        BatchType type = BatchType.from(btchSeCd);
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOrDefault());
        List<BatchExecutionSummary> rows = batchExecutionMapper.selectList(type.getCode());
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 배치 수동 실행
     */
    @Transactional
    public BatchRunResponse run(Long mbrSn, String btchSeCd) {
        BatchType type = BatchType.from(btchSeCd);
        BatchJob job = batchJobs.stream()
                .filter(candidate -> candidate.type() == type)
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.BATCH_NOT_AVAILABLE));
        if (!batchRunner.lock(type)) {
            throw new CustomException(ErrorCode.BATCH_ALREADY_RUNNING);
        }
        BatchExecution execution = BatchExecution.start(type, BatchExecutionType.MANUAL, mbrSn);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    batchRunner.runManual(job, execution);
                } else {
                    batchRunner.unlock(type);
                }
            }
        });
        batchExecutionMapper.insert(execution);
        adminTaskLogService.record(mbrSn, AdminTaskType.PROCESS, TABLE_NAME, execution.getBtchExcnHstrySn(),
                "배치 수동 실행: " + type.getLabel(), null, null);
        return new BatchRunResponse(execution.getBtchExcnHstrySn());
    }

    private boolean hasJob(BatchType type) {
        return batchJobs.stream().anyMatch(job -> job.type() == type);
    }
}
