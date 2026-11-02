package com.nyangmeong.member.service;

import org.springframework.stereotype.Component;

import com.nyangmeong.global.batch.BatchJob;
import com.nyangmeong.global.batch.BatchType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [탈퇴 회원 파기 배치 (FN-ADM-013-02)]
 * - 매일 03:30 KST : 탈퇴 보관일수가 지난 회원의 개인정보 파기 (03:00 AI 대화 파기 뒤)
 * - member 안에서 끝나는 배치라 scheduler 가 아닌 이 도메인에 둠 (docs/convention/architecture.md)
 * - 회원별 트랜잭션 : 한 건이 실패하면 배치 실패로 남기고 다음 실행이 나머지를 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberPurgeJob implements BatchJob {

    private final MemberPurgeService memberPurgeService;

    @Override
    public BatchType type() {
        return BatchType.MEMBER_PURGE;
    }

    @Override
    public String cron() {
        return "0 30 3 * * *";
    }

    @Override
    public Integer execute() {
        int purged = 0;
        for (Long mbrSn : memberPurgeService.searchTargets()) {
            if (memberPurgeService.purge(mbrSn)) {
                purged++;
            }
        }
        log.info("탈퇴 회원 파기 : {}건", purged);
        return purged;
    }
}
