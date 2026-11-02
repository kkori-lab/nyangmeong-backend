package com.nyangmeong.community.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.community.domain.News;
import com.nyangmeong.community.domain.NewsCategory;
import com.nyangmeong.community.domain.NewsKeyword;
import com.nyangmeong.community.domain.NewsSource;
import com.nyangmeong.community.dto.NewsResponse;
import com.nyangmeong.community.dto.NewsSearchCond;
import com.nyangmeong.community.dto.NewsSummary;
import com.nyangmeong.community.mapper.NewsMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.external.NewsFeedClient;
import com.nyangmeong.global.external.NewsFeedItem;
import com.nyangmeong.global.external.NewsFeedProperties;
import com.nyangmeong.global.external.NewsPreview;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [뉴스 서비스]
 * - SCR-CMU-003 목록·상세·수집, 댓글·좋아요·사용자 작성 없음
 * - 수집은 외부 호출이 길어 트랜잭션 없이 실행 : 기사 한 건의 INSERT 가 각자 커밋
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {

    private static final int TITLE_MAX_LENGTH = 200;
    private static final int CONTENT_MAX_LENGTH = 4000;
    private static final int PRESS_MAX_LENGTH = 100;
    private static final int PATH_MAX_LENGTH = 500;
    private static final int RECENT_DAYS = 7;

    private final NewsMapper newsMapper;
    private final NewsFeedClient newsFeedClient;
    private final NewsFeedProperties newsFeedProperties;

    /**
     * 뉴스 목록 조회 (카테고리·키워드는 AND)
     */
    @Transactional(readOnly = true)
    public PageResponse<NewsSummary> search(NewsSearchCond cond, PageQuery pq) {
        String newsClsfCd = trimToNull(cond.getNewsClsfCd());
        if (newsClsfCd != null && !NewsCategory.exists(newsClsfCd)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String keyword = trimToNull(cond.getKeyword());
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOr(10));
        List<NewsSummary> rows = newsMapper.selectList(newsClsfCd, keyword);
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 뉴스 상세 조회
     */
    @Transactional(readOnly = true)
    public NewsResponse getNews(Long newsSn) {
        NewsResponse news = newsMapper.selectNews(newsSn);
        if (news == null) {
            throw new CustomException(ErrorCode.NEWS_NOT_FOUND);
        }
        return news;
    }

    /**
     * 뉴스 수집 : 기준일의 어제와 오늘 기사, 새로 저장한 건수
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int collect(LocalDate today) {
        Map<String, NewsFeedItem> items = new LinkedHashMap<>();
        for (NewsFeedItem item : newsFeedClient.fetchFeed(today.minusDays(1), today.plusDays(1))) {
            if (item.link().length() <= PATH_MAX_LENGTH) {
                items.putIfAbsent(item.link(), item);
            }
        }
        Set<String> saved = items.isEmpty() ? Set.of()
                : new HashSet<>(newsMapper.selectOrgtxtPaths(List.copyOf(items.keySet())));
        List<NewsKeyword> keywords = newsMapper.selectKeywords();
        List<String> excludes = splitKeywords(newsMapper.selectExcludeKeywords());

        NewsDuplicateChecker checker = new NewsDuplicateChecker(
                newsMapper.selectRecentNews(today.minusDays(RECENT_DAYS).atStartOfDay()));

        int created = 0;
        int duplicated = 0;
        int offTopic = 0;
        boolean first = true;
        for (NewsFeedItem item : items.values()) {
            if (saved.contains(item.link())) {
                continue;
            }
            String excluded = findExcludeKeyword(item.title(), excludes);
            if (excluded != null) {
                offTopic++;
                log.info("뉴스 수집 : 주제 무관 건너뜀 '{}' (키워드 '{}')", item.title(), excluded);
                continue;
            }
            String title = cut(item.title(), TITLE_MAX_LENGTH);
            String twin = checker.findDuplicateTitle(title);
            if (twin != null) {
                duplicated++;
                log.info("뉴스 수집 : 중복 건너뜀 '{}' (기존 '{}')", title, twin);
                continue;
            }
            if (!first && !pause()) {
                break;
            }
            first = false;
            NewsPreview preview = newsFeedClient.fetchPreview(item.link());
            News news = toNews(item, preview, keywords);
            if (preview.description() != null && checker.shouldReplaceSummary(title, news.getNewsCn())) {
                news.setNewsCn(title);
            }
            if (save(news)) {
                created++;
                checker.add(news.getNewsTtl(), news.getNewsCn());
            }
        }
        log.info("뉴스 수집 : 피드 {}건 중 새로 저장 {}건, 중복 건너뜀 {}건, 주제 무관 건너뜀 {}건",
                items.size(), created, duplicated, offTopic);
        return created;
    }

    /**
     * 제목에 든 제외 키워드 : 영문 대소문자 무시, 없으면 null
     */
    static String findExcludeKeyword(String title, List<String> keywords) {
        String lowerTitle = title.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (lowerTitle.contains(keyword.toLowerCase(Locale.ROOT))) {
                return keyword;
            }
        }
        return null;
    }

    private static List<String> splitKeywords(List<String> rows) {
        List<String> words = new ArrayList<>();
        for (String row : rows) {
            if (row == null) {
                continue;
            }
            for (String word : row.split(",")) {
                String trimmed = word.trim();
                if (!trimmed.isEmpty()) {
                    words.add(trimmed);
                }
            }
        }
        return words;
    }

    /**
     * 제목 키워드로 카테고리 결정 : 사전 정렬 순서대로 처음 걸린 분류, 없으면 생활
     */
    static String classify(String title, List<NewsKeyword> keywords) {
        for (NewsKeyword keyword : keywords) {
            for (String word : keyword.getKwrdCn().split(",")) {
                String trimmed = word.trim();
                if (!trimmed.isEmpty() && title.contains(trimmed)) {
                    return keyword.getMpngCd();
                }
            }
        }
        return NewsCategory.LIFE.getCode();
    }

    private News toNews(NewsFeedItem item, NewsPreview preview, List<NewsKeyword> keywords) {
        String title = cut(item.title(), TITLE_MAX_LENGTH);
        News news = new News();
        news.setNewsClsfCd(classify(item.title(), keywords));
        news.setNewsSrcCd(NewsSource.PRESS.getCode());
        news.setNewsTtl(title);
        news.setNewsCn(preview.description() != null ? cut(preview.description(), CONTENT_MAX_LENGTH) : title);
        news.setSrcPressNm(item.pressNm() == null ? null : cut(item.pressNm(), PRESS_MAX_LENGTH));
        news.setOrgtxtPath(item.link());
        news.setPblcnDt(item.pblcnDt());
        news.setThmbUrlPath(preview.imageUrl());
        return news;
    }

    private boolean save(News news) {
        try {
            return newsMapper.insert(news) > 0;
        } catch (RuntimeException e) {
            log.warn("뉴스 수집 : 기사 저장 실패 {}", e.toString());
            return false;
        }
    }

    private boolean pause() {
        try {
            Thread.sleep(newsFeedProperties.getRequestIntervalMs());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static String cut(String value, int max) {
        return value.length() > max ? value.substring(0, max) : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
