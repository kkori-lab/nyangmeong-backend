package com.nyangmeong.global.external;

import java.time.LocalDate;
import java.util.List;

/**
 * [뉴스 수집 외부 API]
 * - 검색 RSS 와 원문 미리보기, 미리보기를 읽지 못해도 예외 없이 빈 값
 */
public interface NewsFeedClient {

    /**
     * 기간 안의 기사 목록 : after 포함, before 제외
     */
    List<NewsFeedItem> fetchFeed(LocalDate after, LocalDate before);

    /**
     * 기사의 원문 미리보기
     */
    NewsPreview fetchPreview(String feedLink);
}
