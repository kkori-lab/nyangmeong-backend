package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.community.domain.News;
import com.nyangmeong.community.domain.NewsKeyword;
import com.nyangmeong.community.dto.AdminNewsCreateRequest;
import com.nyangmeong.community.dto.AdminNewsKeywordCreateRequest;
import com.nyangmeong.community.dto.AdminNewsSearchCond;
import com.nyangmeong.community.dto.AdminNewsUpdateRequest;
import com.nyangmeong.community.mapper.NewsMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.response.PageQuery;

/**
 * [검사 - 관리자 펫뉴스 (SCR-ADM-050)]
 */
class AdminNewsServiceTest {

    private static final Long ACTOR = 1L;
    private static final String HEALTH = "CMUC0018";
    private static final String POLICY = "CMUC0020";
    private static final String PRESS = "CMUC0022";
    private static final String ADMIN = "CMUC0023";

    private final NewsMapper newsMapper = mock(NewsMapper.class);
    private final AdminTaskLogService adminTaskLogService = mock(AdminTaskLogService.class);
    private final AdminNewsService service = new AdminNewsService(newsMapper, mock(FileStore.class),
            adminTaskLogService);

    private News news(String srcCd) {
        News news = new News();
        news.setNewsSn(5L);
        news.setNewsSrcCd(srcCd);
        news.setNewsClsfCd(HEALTH);
        news.setNewsTtl("제목");
        news.setNewsCn("요약");
        news.setUseYn("Y");
        return news;
    }

    private void assertError(Runnable call, ErrorCode errorCode) {
        assertThatThrownBy(call::run).isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue("errorCode",
                errorCode);
    }

    @Test
    void 목록_모르는_출처나_카테고리면_400() {
        AdminNewsSearchCond src = new AdminNewsSearchCond();
        src.setNewsSrcCd("CMUC9999");
        AdminNewsSearchCond clsf = new AdminNewsSearchCond();
        clsf.setNewsClsfCd("CMUC0022");

        assertError(() -> service.search(src, new PageQuery(null, null, null)), ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.search(clsf, new PageQuery(null, null, null)), ErrorCode.INVALID_INPUT_VALUE);
        verify(newsMapper, never()).selectAdminList(any(), any());
    }

    @Test
    void 등록_출처는_관리자_등록이고_앞뒤_공백을_지운다() {
        service.create(ACTOR, new AdminNewsCreateRequest("  새 소식  ", "요약", POLICY, 9L));

        ArgumentCaptor<News> captor = ArgumentCaptor.forClass(News.class);
        verify(newsMapper).insertAdmin(captor.capture());
        News saved = captor.getValue();
        assertThat(saved.getNewsSrcCd()).isEqualTo(ADMIN);
        assertThat(saved.getNewsTtl()).isEqualTo("새 소식");
        assertThat(saved.getOrgtxtPath()).isNull();
        assertThat(saved.getAtchFileSn()).isEqualTo(9L);
        assertThat(saved.getPblcnDt()).isNotNull();
        assertThat(saved.getUseYn()).isEqualTo("Y");
    }

    @Test
    void 등록_요약_1000자_초과나_모르는_카테고리면_400() {
        assertError(() -> service.create(ACTOR, new AdminNewsCreateRequest("제목", "가".repeat(1001), HEALTH, null)),
                ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.create(ACTOR, new AdminNewsCreateRequest("제목", "요약", "CMUC0099", null)),
                ErrorCode.INVALID_INPUT_VALUE);
        verify(newsMapper, never()).insertAdmin(any());
    }

    @Test
    void 수정_수집_기사_제목은_400() {
        when(newsMapper.selectById(5L)).thenReturn(news(PRESS));

        assertError(() -> service.update(ACTOR, 5L, new AdminNewsUpdateRequest("바꾼 제목", null, null, null, null)),
                ErrorCode.INVALID_INPUT_VALUE);
        verify(newsMapper, never()).updateAdmin(any());
    }

    @Test
    void 수정_수집_기사_숨김과_카테고리는_바꾼다() {
        when(newsMapper.selectById(5L)).thenReturn(news(PRESS));
        when(newsMapper.updateAdmin(any())).thenReturn(1);

        service.update(ACTOR, 5L, new AdminNewsUpdateRequest(null, null, POLICY, null, false));

        ArgumentCaptor<News> captor = ArgumentCaptor.forClass(News.class);
        verify(newsMapper).updateAdmin(captor.capture());
        assertThat(captor.getValue().getUseYn()).isEqualTo("N");
        assertThat(captor.getValue().getNewsClsfCd()).isEqualTo(POLICY);
        assertThat(captor.getValue().getNewsTtl()).isNull();
    }

    @Test
    void 수정_없는_기사는_404_빈_요청은_400() {
        when(newsMapper.selectById(5L)).thenReturn(null);

        assertError(() -> service.update(ACTOR, 5L, new AdminNewsUpdateRequest(null, null, null, null, true)),
                ErrorCode.NEWS_NOT_FOUND);
        assertError(() -> service.update(ACTOR, 5L, new AdminNewsUpdateRequest(null, null, null, null, null)),
                ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 키워드_쉼표나_51자는_400() {
        assertError(() -> service.createKeyword(ACTOR, new AdminNewsKeywordCreateRequest(HEALTH, "산책,놀이")),
                ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.createKeyword(ACTOR, new AdminNewsKeywordCreateRequest(HEALTH, "가".repeat(51))),
                ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.createKeyword(ACTOR, new AdminNewsKeywordCreateRequest("CMUC0022", "산책")),
                ErrorCode.INVALID_INPUT_VALUE);
        verify(newsMapper, never()).insertKeyword(any(), anyString());
    }

    @Test
    void 키워드_같은_카테고리_중복은_409() {
        when(newsMapper.existsKeyword(HEALTH, "백신")).thenReturn(true);

        assertError(() -> service.createKeyword(ACTOR, new AdminNewsKeywordCreateRequest(HEALTH, " 백신 ")),
                ErrorCode.DUPLICATE_NEWS_KEYWORD);
        verify(newsMapper, never()).insertKeyword(any(), anyString());
    }

    @Test
    void 키워드_등록은_카테고리_정렬_순서를_따른다() {
        when(newsMapper.existsKeyword(HEALTH, "슬개골")).thenReturn(false);
        when(newsMapper.selectKeywordSortSeq(HEALTH)).thenReturn(170);
        when(newsMapper.insertKeyword(any(), eq("1"))).thenReturn(1);

        service.createKeyword(ACTOR, new AdminNewsKeywordCreateRequest(HEALTH, "슬개골"));

        ArgumentCaptor<NewsKeyword> captor = ArgumentCaptor.forClass(NewsKeyword.class);
        verify(newsMapper).insertKeyword(captor.capture(), eq("1"));
        assertThat(captor.getValue().getSortSeq()).isEqualTo(170);
        assertThat(captor.getValue().getMpngCd()).isEqualTo(HEALTH);
        assertThat(captor.getValue().getKwrdCn()).isEqualTo("슬개골");
    }

    @Test
    void 키워드_삭제_없거나_이미_삭제면_404() {
        when(newsMapper.selectKeywordById(3L)).thenReturn(null);

        assertError(() -> service.deleteKeyword(ACTOR, 3L), ErrorCode.NEWS_KEYWORD_NOT_FOUND);
        verify(newsMapper, never()).deleteKeyword(any(), anyString());
    }
}
