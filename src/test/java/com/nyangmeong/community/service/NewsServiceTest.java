package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.nyangmeong.community.domain.News;
import com.nyangmeong.community.domain.NewsKeyword;
import com.nyangmeong.community.dto.NewsResponse;
import com.nyangmeong.community.dto.NewsSearchCond;
import com.nyangmeong.community.mapper.NewsMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.external.NewsFeedClient;
import com.nyangmeong.global.external.NewsFeedItem;
import com.nyangmeong.global.external.NewsFeedProperties;
import com.nyangmeong.global.external.NewsPreview;
import com.nyangmeong.global.response.PageQuery;

/**
 * [검사 - 뉴스 목록·상세·수집]
 */
class NewsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);

    private final NewsMapper newsMapper = mock(NewsMapper.class);
    private final NewsFeedClient feedClient = mock(NewsFeedClient.class);
    private final NewsFeedProperties properties = new NewsFeedProperties();
    private final NewsService service = new NewsService(newsMapper, feedClient, properties);

    @BeforeEach
    void setUp() {
        properties.setRequestIntervalMs(0);
        when(newsMapper.selectKeywords()).thenReturn(List.of(
                keyword("CMUC0018", "건강,백신,진드기"),
                keyword("CMUC0020", "과태료,의무화"),
                keyword("CMUC0021", "여행,동반 출입")));
        when(newsMapper.selectOrgtxtPaths(any())).thenReturn(List.of());
        when(newsMapper.insert(any(News.class))).thenReturn(1);
    }

    @AfterEach
    void clearPage() {
        PageHelper.clearPage();
    }

    private NewsKeyword keyword(String mpngCd, String kwrdCn) {
        NewsKeyword keyword = new NewsKeyword();
        keyword.setMpngCd(mpngCd);
        keyword.setKwrdCn(kwrdCn);
        return keyword;
    }

    private NewsFeedItem item(String title, String link) {
        return new NewsFeedItem(title, link, "전북일보", LocalDateTime.of(2026, 11, 2, 9, 0));
    }

    private News savedNews() {
        ArgumentCaptor<News> captor = ArgumentCaptor.forClass(News.class);
        verify(newsMapper).insert(captor.capture());
        return captor.getValue();
    }

    @Test
    void 조건이_없으면_전체를_조회한다() {
        service.search(new NewsSearchCond(), new PageQuery(null, null, null));

        verify(newsMapper).selectList(null, null);
    }

    @Test
    void 검색어는_다듬어_넘기고_빈_값은_조건_없음() {
        NewsSearchCond cond = new NewsSearchCond();
        cond.setKeyword(" 입양 ");
        service.search(cond, new PageQuery(null, null, null));
        verify(newsMapper).selectList(null, "입양");

        cond.setKeyword("   ");
        service.search(cond, new PageQuery(null, null, null));
        verify(newsMapper).selectList(null, null);
    }

    @Test
    void 공백_카테고리는_조건_없음() {
        NewsSearchCond cond = new NewsSearchCond();
        cond.setNewsClsfCd("  ");

        service.search(cond, new PageQuery(null, null, null));

        verify(newsMapper).selectList(null, null);
    }

    @Test
    void 카테고리_코드는_그대로_넘긴다() {
        NewsSearchCond cond = new NewsSearchCond();
        cond.setNewsClsfCd("CMUC0018");

        service.search(cond, new PageQuery(null, null, null));

        verify(newsMapper).selectList("CMUC0018", null);
    }

    @Test
    void 없는_카테고리_코드는_400() {
        NewsSearchCond cond = new NewsSearchCond();
        cond.setNewsClsfCd("XXXX");

        assertThatThrownBy(() -> service.search(cond, new PageQuery(null, null, null)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        verify(newsMapper, never()).selectList(any(), any());
    }

    @Test
    void 없는_뉴스_번호는_404() {
        when(newsMapper.selectNews(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.getNews(99L))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NEWS_NOT_FOUND));
    }

    @Test
    void 있는_뉴스는_그대로_돌려준다() {
        NewsResponse news = new NewsResponse();
        news.setNewsSn(7L);
        when(newsMapper.selectNews(7L)).thenReturn(news);

        assertThat(service.getNews(7L)).isSameAs(news);
    }

    @Test
    void 제목_키워드로_분류하고_정렬_순서대로_처음_걸린_분류를_쓴다() {
        List<NewsKeyword> keywords = newsMapper.selectKeywords();

        assertThat(NewsService.classify("봄철 진드기 예방접종 안내", keywords)).isEqualTo("CMUC0018");
        assertThat(NewsService.classify("반려동물 등록 의무화, 과태료 강화", keywords)).isEqualTo("CMUC0020");
        assertThat(NewsService.classify("반려동물 동반 출입 숙소 늘어난다", keywords)).isEqualTo("CMUC0021");
        assertThat(NewsService.classify("백신 접종 의무화", keywords)).isEqualTo("CMUC0018");
    }

    @Test
    void 어디에도_안_걸리면_생활로_넣는다() {
        assertThat(NewsService.classify("강아지 산책 코스 소개", newsMapper.selectKeywords())).isEqualTo("CMUC0019");
        assertThat(NewsService.classify("제목", List.of())).isEqualTo("CMUC0019");
    }

    @Test
    void 수집은_어제부터_내일_전날까지_범위로_피드를_부른다() {
        when(feedClient.fetchFeed(any(), any())).thenReturn(List.of());

        int created = service.collect(TODAY);

        assertThat(created).isZero();
        verify(feedClient).fetchFeed(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3));
        verify(newsMapper, never()).selectOrgtxtPaths(any());
    }

    @Test
    void 이미_저장한_기사는_원문_미리보기를_부르지_않는다() {
        when(feedClient.fetchFeed(any(), any())).thenReturn(List.of(
                item("이미 있는 기사", "https://news.google.com/rss/articles/old"),
                item("새 기사", "https://news.google.com/rss/articles/new")));
        when(newsMapper.selectOrgtxtPaths(any())).thenReturn(List.of("https://news.google.com/rss/articles/old"));
        when(feedClient.fetchPreview(anyString())).thenReturn(NewsPreview.empty());

        int created = service.collect(TODAY);

        assertThat(created).isEqualTo(1);
        verify(feedClient, never()).fetchPreview("https://news.google.com/rss/articles/old");
        verify(feedClient).fetchPreview("https://news.google.com/rss/articles/new");
    }

    @Test
    void 같은_링크가_피드에_두_번_있으면_한_번만_처리한다() {
        when(feedClient.fetchFeed(any(), any())).thenReturn(List.of(
                item("기사", "https://news.google.com/rss/articles/a"),
                item("기사", "https://news.google.com/rss/articles/a")));
        when(feedClient.fetchPreview(anyString())).thenReturn(NewsPreview.empty());

        assertThat(service.collect(TODAY)).isEqualTo(1);

        verify(newsMapper, times(1)).insert(any(News.class));
    }

    @Test
    void 원문_미리보기가_있으면_요약과_썸네일로_저장한다() {
        when(feedClient.fetchFeed(any(), any())).thenReturn(List.of(
                item("전북 유기동물 입양 캠페인", "https://news.google.com/rss/articles/a")));
        when(feedClient.fetchPreview(anyString()))
                .thenReturn(new NewsPreview("입양 절차를 간소화한다", "https://img.example.com/a.jpg"));

        service.collect(TODAY);

        News news = savedNews();
        assertThat(news.getNewsCn()).isEqualTo("입양 절차를 간소화한다");
        assertThat(news.getThmbUrlPath()).isEqualTo("https://img.example.com/a.jpg");
        assertThat(news.getNewsSrcCd()).isEqualTo("CMUC0022");
        assertThat(news.getSrcPressNm()).isEqualTo("전북일보");
        assertThat(news.getOrgtxtPath()).isEqualTo("https://news.google.com/rss/articles/a");
        assertThat(news.getPblcnDt()).isEqualTo(LocalDateTime.of(2026, 11, 2, 9, 0));
    }

    @Test
    void 원문_미리보기를_읽지_못하면_요약은_제목_썸네일은_null() {
        when(feedClient.fetchFeed(any(), any())).thenReturn(List.of(
                item("반려동물 등록 의무화, 과태료 강화", "https://news.google.com/rss/articles/a")));
        when(feedClient.fetchPreview(anyString())).thenReturn(NewsPreview.empty());

        service.collect(TODAY);

        News news = savedNews();
        assertThat(news.getNewsCn()).isEqualTo("반려동물 등록 의무화, 과태료 강화");
        assertThat(news.getThmbUrlPath()).isNull();
        assertThat(news.getNewsClsfCd()).isEqualTo("CMUC0020");
    }

    @Test
    void 긴_제목과_요약은_컬럼_길이로_자른다() {
        when(feedClient.fetchFeed(any(), any())).thenReturn(List.of(
                item("가".repeat(300), "https://news.google.com/rss/articles/a")));
        when(feedClient.fetchPreview(anyString())).thenReturn(new NewsPreview("나".repeat(5000), null));

        service.collect(TODAY);

        News news = savedNews();
        assertThat(news.getNewsTtl()).hasSize(200);
        assertThat(news.getNewsCn()).hasSize(4000);
    }

    @Test
    void 건너뛴_기사와_저장_실패는_세지_않고_나머지는_계속_저장한다() {
        when(feedClient.fetchFeed(any(), any())).thenReturn(List.of(
                item("중복 기사", "https://news.google.com/rss/articles/a"),
                item("오류 기사", "https://news.google.com/rss/articles/b"),
                item("정상 기사", "https://news.google.com/rss/articles/c")));
        when(feedClient.fetchPreview(anyString())).thenReturn(NewsPreview.empty());
        when(newsMapper.insert(any(News.class))).thenReturn(0).thenThrow(new IllegalStateException("fail"))
                .thenReturn(1);

        int created = service.collect(TODAY);

        assertThat(created).isEqualTo(1);
        verify(newsMapper, times(3)).insert(any(News.class));
    }

    @Test
    void 수집은_외부_호출이_길어_트랜잭션_없이_실행한다() throws Exception {
        Transactional transactional = NewsService.class.getMethod("collect", LocalDate.class)
                .getAnnotation(Transactional.class);

        assertThat(transactional.propagation()).isEqualTo(Propagation.NOT_SUPPORTED);
    }
}
