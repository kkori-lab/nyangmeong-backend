package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.community.domain.Comment;
import com.nyangmeong.community.dto.AdminCommentDeleteRequest;
import com.nyangmeong.community.dto.AdminCommentSearchCond;
import com.nyangmeong.community.mapper.CommentMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.response.PageQuery;

/**
 * [검사 - 관리자 댓글 조회·삭제]
 */
class AdminCommentServiceTest {

    private static final Long ACTOR = 1L;
    private static final Long CMNT = 9L;
    private static final AdminCommentDeleteRequest REQ = new AdminCommentDeleteRequest("욕설");

    private final CommentMapper commentMapper = mock(CommentMapper.class);
    private final AdminTaskLogService adminTaskLogService = mock(AdminTaskLogService.class);
    private final AdminCommentService service = new AdminCommentService(commentMapper, adminTaskLogService);

    private Comment comment(String delYn) {
        Comment comment = new Comment();
        comment.setCmntSn(CMNT);
        comment.setDelYn(delYn);
        return comment;
    }

    private void assertError(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run).isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue("errorCode",
                code);
    }

    @Test
    void 검색어의_와일드카드는_글자로_검색한다() {
        AdminCommentSearchCond cond = new AdminCommentSearchCond();
        cond.setQ("a_b");
        when(commentMapper.selectAdminList(any(), any())).thenReturn(List.of());

        service.search(cond, new PageQuery(null, null, null));

        verify(commentMapper).selectAdminList(cond, "a\\_b");
    }

    @Test
    void 삭제하면_사유를_감사_로그에_남긴다() {
        when(commentMapper.selectAdminComment(CMNT)).thenReturn(comment("N"));
        when(commentMapper.updateDeleted(CMNT)).thenReturn(1);

        service.delete(ACTOR, CMNT, REQ);

        verify(adminTaskLogService).record(eq(ACTOR), eq(AdminTaskType.DELETE), eq("CMNT"), eq(CMNT), anyString(),
                any(), any());
    }

    @Test
    void 없는_댓글은_404() {
        assertError(() -> service.delete(ACTOR, CMNT, REQ), ErrorCode.COMMENT_NOT_FOUND);
    }

    @Test
    void 이미_삭제된_댓글은_409() {
        when(commentMapper.selectAdminComment(CMNT)).thenReturn(comment("Y"));

        assertError(() -> service.delete(ACTOR, CMNT, REQ), ErrorCode.ALREADY_PROCESSED);
        verify(commentMapper, never()).updateDeleted(anyLong());
    }

    @Test
    void 동시에_삭제되면_감사_로그를_남기지_않는다() {
        when(commentMapper.selectAdminComment(CMNT)).thenReturn(comment("N"));
        when(commentMapper.updateDeleted(CMNT)).thenReturn(0);

        assertError(() -> service.delete(ACTOR, CMNT, REQ), ErrorCode.ALREADY_PROCESSED);
        verify(adminTaskLogService, never()).record(any(), any(), any(), any(), any(), any(), any());
    }
}
