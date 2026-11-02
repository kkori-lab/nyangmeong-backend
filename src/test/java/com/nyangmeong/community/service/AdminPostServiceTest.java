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

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.community.domain.Post;
import com.nyangmeong.community.domain.PostStatusHistory;
import com.nyangmeong.community.dto.AdminPostDetailResponse;
import com.nyangmeong.community.dto.AdminPostSearchCond;
import com.nyangmeong.community.dto.AdminPostStatusChangeRequest;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.community.mapper.ReportMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.response.PageQuery;

/**
 * [검사 - 관리자 게시글 검색·상세·블라인드·복구]
 */
class AdminPostServiceTest {

    private static final Long ACTOR = 1L;
    private static final Long PST = 7L;
    private static final String NORMAL = "CMUC0003";
    private static final String BLIND = "CMUC0004";
    private static final String DELETED = "CMUC0005";
    private static final AdminPostStatusChangeRequest REQ = new AdminPostStatusChangeRequest("광고");

    private final PostMapper postMapper = mock(PostMapper.class);
    private final ReportMapper reportMapper = mock(ReportMapper.class);
    private final AdminTaskLogService adminTaskLogService = mock(AdminTaskLogService.class);
    private final AdminPostService service = new AdminPostService(postMapper, reportMapper, adminTaskLogService);

    private Post post(String sttsCd) {
        Post post = new Post();
        post.setPstSn(PST);
        post.setMbrSn(2L);
        post.setPstSttsCd(sttsCd);
        return post;
    }

    private void assertError(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run).isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue("errorCode",
                code);
    }

    @Test
    void 시작일이_종료일보다_늦으면_400() {
        AdminPostSearchCond cond = new AdminPostSearchCond();
        cond.setBgngYmd(LocalDate.of(2026, 11, 2));
        cond.setEndYmd(LocalDate.of(2026, 11, 1));

        assertError(() -> service.search(cond, new PageQuery(null, null, null)), ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 없는_게시판_코드는_400() {
        AdminPostSearchCond cond = new AdminPostSearchCond();
        cond.setBbsCd("CMUC9999");

        assertError(() -> service.search(cond, new PageQuery(null, null, null)), ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 없는_상태_코드는_400() {
        AdminPostSearchCond cond = new AdminPostSearchCond();
        cond.setStatus("CMUC9999");

        assertError(() -> service.search(cond, new PageQuery(null, null, null)), ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 검색어의_와일드카드는_글자로_검색하고_실종센터도_거를_수_있다() {
        AdminPostSearchCond cond = new AdminPostSearchCond();
        cond.setQ(" 100% ");
        cond.setBbsCd("CMUC0024");
        when(postMapper.selectAdminList(any(), any())).thenReturn(List.of());

        service.search(cond, new PageQuery(null, null, null));

        verify(postMapper).selectAdminList(cond, "100\\%");
    }

    @Test
    void 삭제된_글도_상세를_돌려준다() {
        when(postMapper.selectAdminPost(PST)).thenReturn(post(DELETED));
        when(reportMapper.countReports(PST, null)).thenReturn(6);
        when(postMapper.selectImages(PST)).thenReturn(List.of());
        when(postMapper.selectAdminStatusHistories(PST)).thenReturn(List.of());

        AdminPostDetailResponse response = service.get(PST);

        assertThat(response.pstSttsCd()).isEqualTo(DELETED);
        assertThat(response.dclrCnt()).isEqualTo(6);
    }

    @Test
    void 없는_글은_404() {
        assertError(() -> service.get(PST), ErrorCode.POST_NOT_FOUND);
    }

    @Test
    void 블라인드하면_이력과_감사_로그를_남긴다() {
        when(postMapper.selectAdminPost(PST)).thenReturn(post(NORMAL));
        when(postMapper.updateStatus(PST, NORMAL, BLIND, "1")).thenReturn(1);

        service.blind(ACTOR, PST, REQ);

        ArgumentCaptor<PostStatusHistory> history = ArgumentCaptor.forClass(PostStatusHistory.class);
        verify(postMapper).insertStatusHistory(history.capture());
        assertThat(history.getValue().getMbrSn()).isEqualTo(ACTOR);
        assertThat(history.getValue().getBfrSttsCd()).isEqualTo(NORMAL);
        assertThat(history.getValue().getAftrSttsCd()).isEqualTo(BLIND);
        assertThat(history.getValue().getChgRsn()).isEqualTo("광고");
        verify(adminTaskLogService).record(eq(ACTOR), eq(AdminTaskType.PROCESS), eq("PST"), eq(PST), anyString(),
                any(), any());
    }

    @Test
    void 복구하면_정상으로_돌리고_이력을_남긴다() {
        when(postMapper.selectAdminPost(PST)).thenReturn(post(BLIND));
        when(postMapper.updateStatus(PST, BLIND, NORMAL, "1")).thenReturn(1);

        service.restore(ACTOR, PST, REQ);

        ArgumentCaptor<PostStatusHistory> history = ArgumentCaptor.forClass(PostStatusHistory.class);
        verify(postMapper).insertStatusHistory(history.capture());
        assertThat(history.getValue().getAftrSttsCd()).isEqualTo(NORMAL);
    }

    @Test
    void 정상_글이_아니면_블라인드할_수_없다() {
        when(postMapper.selectAdminPost(PST)).thenReturn(post(DELETED));

        assertError(() -> service.blind(ACTOR, PST, REQ), ErrorCode.ALREADY_PROCESSED);
        verify(postMapper, never()).updateStatus(any(), any(), any(), any());
    }

    @Test
    void 블라인드_글이_아니면_복구할_수_없다() {
        when(postMapper.selectAdminPost(PST)).thenReturn(post(DELETED));

        assertError(() -> service.restore(ACTOR, PST, REQ), ErrorCode.ALREADY_PROCESSED);
    }

    @Test
    void 동시에_상태가_바뀌면_이력을_남기지_않는다() {
        when(postMapper.selectAdminPost(PST)).thenReturn(post(NORMAL));
        when(postMapper.updateStatus(PST, NORMAL, BLIND, "1")).thenReturn(0);

        assertError(() -> service.blind(ACTOR, PST, REQ), ErrorCode.ALREADY_PROCESSED);
        verify(postMapper, never()).insertStatusHistory(any());
    }
}
