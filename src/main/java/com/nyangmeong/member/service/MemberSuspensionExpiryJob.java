package com.nyangmeong.member.service;

import org.springframework.stereotype.Component;

import com.nyangmeong.global.batch.BatchJob;
import com.nyangmeong.global.batch.BatchType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [회원 정지 만료 해제 배치 (FN-ADM-012-04)]
 * - 매일 00:00 KST : 정지 종료 일시가 지난 회원 정상 전환
 * - member 안에서 끝나는 배치라 scheduler 가 아닌 이 도메인에 둠 (docs/convention/architecture.md)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberSuspensionExpiryJob implements BatchJob {

    private final MemberSuspensionService memberSuspensionService;

    @Override
    public BatchType type() {
        return BatchType.MEMBER_SUSPENSION_EXPIRY;
    }

    @Override
    public String cron() {
        return "0 0 0 * * *";
    }

    @Override
    public Integer execute() {
        int released = memberSuspensionService.releaseExpired();
        log.info("회원 정지 만료 해제 : {}건", released);
        return released;
    }
}
