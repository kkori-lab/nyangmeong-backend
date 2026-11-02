package com.nyangmeong.health.service;

import org.springframework.stereotype.Component;

import com.nyangmeong.global.batch.BatchJob;
import com.nyangmeong.global.batch.BatchType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [처방약 복용 종료 배치 (FN-HLT-005-03)]
 * - 매일 00:00 KST : 복용 종료일이 지난 복용중 처방약 종료 + 투약 케어 항목 삭제
 * - health 안에서 끝나는 배치라 scheduler 가 아닌 이 도메인에 둠 (docs/convention/architecture.md)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrescriptionExpiryJob implements BatchJob {

    private final PrescriptionService prescriptionService;

    @Override
    public BatchType type() {
        return BatchType.PRESCRIPTION_EXPIRY;
    }

    @Override
    public String cron() {
        return "0 0 0 * * *";
    }

    @Override
    public Integer execute() {
        int ended = prescriptionService.updateExpired();
        log.info("처방약 복용 종료 : {}건", ended);
        return ended;
    }
}
