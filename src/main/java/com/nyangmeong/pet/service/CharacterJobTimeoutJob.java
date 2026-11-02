package com.nyangmeong.pet.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.nyangmeong.global.batch.BatchJob;
import com.nyangmeong.global.batch.BatchType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [캐릭터 생성 시간 초과 배치 (FN-MYP-010-12)]
 * - 5분마다 : 결과 보고가 끊긴 작업을 실패로 정리 -> 기본 캐릭터 표시, 다시 요청 가능
 * - pet 안에서 끝나는 배치라 scheduler 가 아닌 이 도메인에 둠 (docs/convention/architecture.md)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CharacterJobTimeoutJob implements BatchJob {

    static final String TIMEOUT = "CHARACTER_TIMEOUT";

    private final CharacterCreationService characterCreationService;

    @Override
    public BatchType type() {
        return BatchType.CHARACTER_JOB_TIMEOUT;
    }

    @Override
    public String cron() {
        return "0 */5 * * * *";
    }

    @Override
    public Integer execute() {
        List<Long> charCrtJobSns = characterCreationService.searchTimedOutJobs();
        for (Long charCrtJobSn : charCrtJobSns) {
            characterCreationService.fail(charCrtJobSn, TIMEOUT);
        }
        log.info("캐릭터 생성 시간 초과 : {}건", charCrtJobSns.size());
        return charCrtJobSns.size();
    }
}
