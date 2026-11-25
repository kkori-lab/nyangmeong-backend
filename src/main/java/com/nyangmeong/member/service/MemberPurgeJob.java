package com.nyangmeong.member.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.nyangmeong.global.batch.BatchJob;
import com.nyangmeong.global.batch.BatchType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [탈퇴 회원 파기 배치 (FN-ADM-013-02)]
 * - 매일 03:30 KST : 탈퇴 보관일수가 지난 회원의 개인정보 파기 (03:00 AI 대화 파기 뒤)
 * - member 안에서 끝나는 배치라 scheduler 가 아닌 이 도메인에 둠 (docs/convention/architecture.md)
 * - 회원별 트랜잭션 : 한 회원이 실패해도 다음 회원을 계속 파기하고, 실패가 있으면 끝에 예외를 던져 배치 결과를 실패로 남긴다
 *   (성공·실패 건수와 실패 회원 번호가 실패 사유에 들어간다. 실패한 회원은 다음 실행이 다시 처리)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberPurgeJob implements BatchJob {

    /** 실패 사유(최대 1000자)에 적을 실패 회원 번호 수 */
    private static final int FAIL_MBR_SHOW_MAX = 20;

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
        List<Long> failedMbrSns = new ArrayList<>();
        for (Long mbrSn : memberPurgeService.searchTargets()) {
            try {
                if (memberPurgeService.purge(mbrSn)) {
                    purged++;
                }
            } catch (RuntimeException e) {
                log.error("탈퇴 회원 파기 실패 mbrSn={}", mbrSn, e);
                failedMbrSns.add(mbrSn);
            }
        }
        log.info("탈퇴 회원 파기 : {}건, 실패 {}건", purged, failedMbrSns.size());
        if (!failedMbrSns.isEmpty()) {
            throw new IllegalStateException(failMessage(purged, failedMbrSns));
        }
        return purged;
    }

    private static String failMessage(int purged, List<Long> failedMbrSns) {
        String shown = failedMbrSns.stream().limit(FAIL_MBR_SHOW_MAX).map(String::valueOf)
                .collect(Collectors.joining(", "));
        String more = failedMbrSns.size() > FAIL_MBR_SHOW_MAX ? " 외 " + (failedMbrSns.size() - FAIL_MBR_SHOW_MAX) + "건" : "";
        return "탈퇴 회원 파기 성공 %d건, 실패 %d건 (mbrSn %s%s)".formatted(purged, failedMbrSns.size(), shown, more);
    }
}
