package com.nyangmeong.community.service;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.nyangmeong.global.batch.BatchJob;
import com.nyangmeong.global.batch.BatchType;

import lombok.RequiredArgsConstructor;

/**
 * [뉴스 수집 배치 (FN-CMU-003-06)]
 * - 매일 06시·18시에 어제~오늘 기사 수집, 이미 저장한 기사는 건너뜀
 * - app.news.batch-enabled=true 일 때만 등록 : 배포(prod)는 켬, 로컬은 확인할 때만 켬(팀원 PC마다 공용 DB에 쓰지 않게)
 */
@Component
@ConditionalOnProperty(name = "app.news.batch-enabled", havingValue = "true")
@RequiredArgsConstructor
public class NewsCollectJob implements BatchJob {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final NewsService newsService;

    @Override
    public BatchType type() {
        return BatchType.NEWS_COLLECT;
    }

    @Override
    public String cron() {
        return "0 0 6,18 * * *";
    }

    @Override
    public Integer execute() {
        return newsService.collect(LocalDate.now(ZONE));
    }
}
