package com.nyangmeong.global.log;

import org.springframework.stereotype.Component;

import com.nyangmeong.global.batch.BatchJob;
import com.nyangmeong.global.batch.BatchType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [감사 로그 정리 배치 (FN-ADM-114-04)]
 * - 매일 03:30 KST : 보관 일수(30일)가 지난 관리자 작업 로그를 물리 삭제
 * - 관리자 화면에는 삭제 API가 없고 이 배치만 지운다. 지운 일 자체는 감사 로그에 남기지 않는다(배치 실행 이력에 지운 행 수가 남는다)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogPurgeJob implements BatchJob {

    private final AdminTaskLogService adminTaskLogService;

    @Override
    public BatchType type() {
        return BatchType.AUDIT_LOG_PURGE;
    }

    @Override
    public String cron() {
        return "0 30 3 * * *";
    }

    @Override
    public Integer execute() {
        int deleted = adminTaskLogService.purgeExpired();
        log.info("감사 로그 정리 : {}건 삭제", deleted);
        return deleted;
    }
}
