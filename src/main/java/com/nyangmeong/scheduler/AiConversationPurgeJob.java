package com.nyangmeong.scheduler;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.ai.service.AiConversationService;
import com.nyangmeong.global.batch.BatchJob;
import com.nyangmeong.global.batch.BatchType;
import com.nyangmeong.global.setting.SystemSettingService;
import com.nyangmeong.member.service.MemberService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [탈퇴 회원 AI 대화 파기 배치 (FN-AIM-011-19)]
 * - 매일 03:00 KST : 탈퇴 보관일수(SYS_STNG.WHDWL_MBR_KPNG_DAY_CNT)가 지난 회원의 대화 물리 삭제
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiConversationPurgeJob implements BatchJob {

    private final SystemSettingService systemSettingService;
    private final MemberService memberService;
    private final AiConversationService aiConversationService;

    @Override
    public BatchType type() {
        return BatchType.AI_CONVERSATION_PURGE;
    }

    @Override
    public String cron() {
        return "0 0 3 * * *";
    }

    @Override
    @Transactional
    public Integer execute() {
        return purge();
    }

    /**
     * 보관일수 경과 탈퇴 회원 대화 파기
     */
    @Transactional
    public int purge() {
        int keepDayCnt = systemSettingService.get().getWhdwlMbrKpngDayCnt().intValue();
        List<Long> mbrSns = memberService.searchExpiredWithdrawn(keepDayCnt);
        int deleted = aiConversationService.deleteByMbrSns(mbrSns);
        log.info("탈퇴 회원 AI 대화 파기 : 회원 {}명, 대화 {}건", mbrSns.size(), deleted);
        return deleted;
    }
}
