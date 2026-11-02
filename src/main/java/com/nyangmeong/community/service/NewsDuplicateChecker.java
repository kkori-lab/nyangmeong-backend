package com.nyangmeong.community.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.nyangmeong.community.domain.News;

/**
 * [뉴스 수집 중복 판정]
 * - 수집 한 번마다 최근 기사로 만들고, 저장한 기사를 더해 가며 비교
 * - 같은 제목·비슷한 제목은 기사째 건너뛰고, 요약이 엉뚱한 기사는 요약만 제목으로 바꿈
 */
class NewsDuplicateChecker {

    static final double SIMILARITY_THRESHOLD = 0.6;
    static final int SIMILARITY_MIN_LENGTH = 8;
    private static final int WORD_MIN_LENGTH = 2;

    private static final Pattern HEAD_TAGS = Pattern.compile("^(\\s*(\\[[^\\]]*\\]|【[^】]*】))+");
    private static final Pattern NON_TEXT = Pattern.compile("[^\\p{L}\\p{N}]");

    private record Entry(String title, String normTitle, String summary) {
    }

    private final List<Entry> entries = new ArrayList<>();
    private final Map<String, Entry> bySummary = new HashMap<>();

    NewsDuplicateChecker(List<News> recent) {
        for (News news : recent) {
            if (news.getNewsTtl() != null) {
                add(news.getNewsTtl(), news.getNewsCn());
            }
        }
    }

    /**
     * 제목 정규화 : 대괄호 머리말과 글자·숫자 외 문자를 지우고 소문자로
     */
    static String normalize(String text) {
        String stripped = HEAD_TAGS.matcher(text).replaceFirst("");
        return NON_TEXT.matcher(stripped).replaceAll("").toLowerCase();
    }

    /**
     * 글자 2-gram 다이스 계수
     */
    static double similarity(String a, String b) {
        if (a.length() < 2 || b.length() < 2) {
            return a.equals(b) ? 1.0 : 0.0;
        }
        Map<String, Integer> grams = new HashMap<>();
        for (int i = 0; i < a.length() - 1; i++) {
            grams.merge(a.substring(i, i + 2), 1, Integer::sum);
        }
        int common = 0;
        for (int i = 0; i < b.length() - 1; i++) {
            Integer left = grams.get(b.substring(i, i + 2));
            if (left != null && left > 0) {
                grams.put(b.substring(i, i + 2), left - 1);
                common++;
            }
        }
        return 2.0 * common / ((a.length() - 1) + (b.length() - 1));
    }

    /**
     * 같거나 비슷한 제목의 기존 기사 제목, 없으면 null
     */
    String findDuplicateTitle(String title) {
        String norm = normalize(title);
        for (Entry entry : entries) {
            if (entry.normTitle().equals(norm)) {
                return entry.title();
            }
            if (norm.length() >= SIMILARITY_MIN_LENGTH && entry.normTitle().length() >= SIMILARITY_MIN_LENGTH
                    && similarity(norm, entry.normTitle()) >= SIMILARITY_THRESHOLD) {
                return entry.title();
            }
        }
        return null;
    }

    /**
     * 요약을 제목으로 바꿔야 하는지 : 제목이 다른 기사와 같은 요약, 또는 제목과 겹치는 낱말이 없는 요약
     */
    boolean shouldReplaceSummary(String title, String summary) {
        String trimmed = summary.trim();
        Entry same = bySummary.get(trimmed);
        if (same != null && !same.normTitle().equals(normalize(title))) {
            return true;
        }
        String normSummary = normalize(summary);
        for (String word : title.split("\\s+")) {
            String norm = normalize(word);
            if (norm.length() >= WORD_MIN_LENGTH && normSummary.contains(norm)) {
                return false;
            }
        }
        return true;
    }

    void add(String title, String summary) {
        Entry entry = new Entry(title, normalize(title), summary);
        entries.add(entry);
        if (summary != null) {
            bySummary.putIfAbsent(summary.trim(), entry);
        }
    }
}
