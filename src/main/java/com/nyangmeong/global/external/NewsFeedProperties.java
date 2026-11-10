package com.nyangmeong.global.external;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * [뉴스 수집 설정]
 * - app.news.rss-url : 검색 RSS 주소, app.news.query : 검색어(OR 로 연결)
 * - app.news.request-interval-ms : 기사 사이 대기, app.news.timeout-ms : 요청 한 번의 제한 시간
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.news")
public class NewsFeedProperties {

    /** 검색 RSS 주소 */
    private String rssUrl = "https://news.google.com/rss/search";

    /** 검색어 : 반려 낱말만(띄어 쓴 "반려 동물" 포함). 강아지·고양이 단독 검색어는 게임·웹소설 같은 무관 기사를 끌어와 쓰지 않는다 */
    private String query = "반려동물 OR 반려견 OR 반려묘 OR \"반려 동물\"";

    /** 기사 사이 대기(밀리초) */
    private long requestIntervalMs = 1000;

    /** 요청 제한 시간(밀리초) */
    private long timeoutMs = 5000;
}
