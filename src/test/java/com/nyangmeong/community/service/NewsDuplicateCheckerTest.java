package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.community.domain.News;

/**
 * [검사 - 뉴스 수집 중복 판정]
 */
class NewsDuplicateCheckerTest {

    private News news(String title, String content) {
        News news = new News();
        news.setNewsTtl(title);
        news.setNewsCn(content);
        return news;
    }

    @Test
    void 제목은_머리말과_특수문자를_지우고_소문자로_정규화한다() {
        assertThat(NewsDuplicateChecker.normalize("[포토] 여름철 산책, 시간 추천 '관련 소식'"))
                .isEqualTo("여름철산책시간추천관련소식");
        assertThat(NewsDuplicateChecker.normalize("【단독】 Dog Show 2026…")).isEqualTo("dogshow2026");
    }

    @Test
    void 정규화한_제목이_같으면_중복이다() {
        NewsDuplicateChecker checker = new NewsDuplicateChecker(
                List.of(news("여름철 산책 시간 추천 관련 소식", "요약")));

        assertThat(checker.findDuplicateTitle("[포토] 여름철 산책, 시간 추천 '관련 소식'"))
                .isEqualTo("여름철 산책 시간 추천 관련 소식");
    }

    @Test
    void 비슷한_제목은_중복이다() {
        NewsDuplicateChecker checker = new NewsDuplicateChecker(
                List.of(news("힐스코리아, 반려견 사료 4종 선보여...소비자 후기 공개", "요약")));

        assertThat(checker.findDuplicateTitle("힐스코리아 반려견 사료 4종 소비자 후기 인기")).isNotNull();
    }

    @Test
    void 주제가_다른_제목은_중복이_아니다() {
        NewsDuplicateChecker checker = new NewsDuplicateChecker(
                List.of(news("전북 유기동물 입양 캠페인 시작", "요약")));

        assertThat(checker.findDuplicateTitle("강아지 노령견 식단 관리 요령 소개")).isNull();
    }

    @Test
    void 짧은_제목은_유사도를_보지_않는다() {
        NewsDuplicateChecker checker = new NewsDuplicateChecker(List.of(news("강아지 산책", "요약")));

        assertThat(checker.findDuplicateTitle("강아지 산책로")).isNull();
        assertThat(checker.findDuplicateTitle("강아지 산책!")).isEqualTo("강아지 산책");
    }

    @Test
    void 저장한_기사도_다음_비교_대상이_된다() {
        NewsDuplicateChecker checker = new NewsDuplicateChecker(List.of());
        assertThat(checker.findDuplicateTitle("새 기사 제목입니다")).isNull();

        checker.add("새 기사 제목입니다", "요약");

        assertThat(checker.findDuplicateTitle("새 기사 제목입니다")).isNotNull();
    }

    @Test
    void 다른_제목과_요약이_같으면_요약을_바꾼다() {
        NewsDuplicateChecker checker = new NewsDuplicateChecker(
                List.of(news("고양이 건강 검진 안내", "고양이 건강 검진 안내 공통 문구")));

        assertThat(checker.shouldReplaceSummary("강아지 산책 코스", "고양이 건강 검진 안내 공통 문구")).isTrue();
    }

    @Test
    void 제목과_겹치는_낱말이_없는_요약은_바꾼다() {
        NewsDuplicateChecker checker = new NewsDuplicateChecker(List.of());

        assertThat(checker.shouldReplaceSummary("반려동물 보험 가입 늘어", "경제/금융 핵심 정리 VOD 클립영상")).isTrue();
    }

    @Test
    void 제목과_겹치는_낱말이_있으면_요약을_유지한다() {
        NewsDuplicateChecker checker = new NewsDuplicateChecker(List.of());

        assertThat(checker.shouldReplaceSummary("반려동물 보험 가입 늘어", "보험사가 새 상품을 내놓았다")).isFalse();
    }
}
