package com.nyangmeong.community.service;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.nyangmeong.global.batch.BatchJob;
import com.nyangmeong.global.batch.BatchType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [오늘의 한마디 선택 배치 (FN-ADM-023-05)]
 * - 매일 00:00 KST : 사용 중 후보 문구 1건을 골라 그날 CMNTY_AI_CMTRY 에 저장
 * - community 안에서 끝나는 배치라 scheduler 가 아닌 이 도메인에 둠 (docs/convention/architecture.md)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TodayPhraseSelectJob implements BatchJob {

    private final AdminExposureService adminExposureService;

    @Override
    public BatchType type() {
        return BatchType.TODAY_PHRASE_SELECT;
    }

    @Override
    public String cron() {
        return "0 0 0 * * *";
    }

    @Override
    public Integer execute() {
        int selected = adminExposureService.selectTodayPhrase(LocalDate.now()) ? 1 : 0;
        log.info("오늘의 한마디 선택 : {}건", selected);
        return selected;
    }
}
