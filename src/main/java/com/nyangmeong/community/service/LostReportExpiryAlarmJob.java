package com.nyangmeong.community.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.nyangmeong.global.batch.BatchJob;
import com.nyangmeong.global.batch.BatchType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [실종 만료 사전 알림 배치 (FN-CMU-002-09)]
 * - 매일 09:00 KST : 실종 상태 제보 중 종료일이 7일 뒤·1일 뒤인 글의 작성자에게 알림
 * - 연장하면 종료일이 바뀌어 이전 종료일 기준 알림은 나가지 않는다. 같은 날 다시 돌리면 다시 보낸다(사람 결정 2026-11-06)
 * - 글마다 따로 트랜잭션 : 한 건이 실패해도 나머지는 처리
 * - community 안에서 끝나는 배치라 scheduler 가 아닌 이 도메인에 둠 (docs/convention/architecture.md)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LostReportExpiryAlarmJob implements BatchJob {

    private static final List<Integer> DAYS = List.of(7, 1);

    private final LostReportService lostReportService;

    @Override
    public BatchType type() {
        return BatchType.LOST_EXPIRY_ALARM;
    }

    @Override
    public String cron() {
        return "0 0 9 * * *";
    }

    @Override
    public Integer execute() {
        int processed = 0;
        for (int days : DAYS) {
            for (Long lostTpofSn : lostReportService.findExpiryAlarmTargets(days)) {
                try {
                    if (lostReportService.sendExpiryAlarm(lostTpofSn, days)) {
                        processed++;
                    }
                } catch (RuntimeException e) {
                    log.warn("실종 만료 사전 알림 실패 : lostTpofSn={}, days={}", lostTpofSn, days, e);
                }
            }
        }
        log.info("실종 만료 사전 알림 : {}건", processed);
        return processed;
    }
}
