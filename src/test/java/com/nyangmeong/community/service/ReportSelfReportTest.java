package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.nyangmeong.community.domain.Comment;
import com.nyangmeong.community.domain.Post;
import com.nyangmeong.community.domain.Report;
import com.nyangmeong.community.dto.ReportCreateRequest;
import com.nyangmeong.community.dto.ReportResponse;
import com.nyangmeong.community.mapper.CommentMapper;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.community.mapper.ReportMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.setting.SystemSetting;
import com.nyangmeong.global.setting.SystemSettingService;
import com.nyangmeong.member.service.MemberService;
import com.nyangmeong.member.service.MemberSuspensionService;

/**
 * [검사 - 자기 글·댓글 신고 차단]
 */
class ReportSelfReportTest {

    private static final Long ME = 1L;
    private static final Long OTHER = 2L;
    private static final Long PST = 7L;
    private static final Long CMNT = 20L;
    private static final String NORMAL = "CMUC0003";

    private final ReportMapper reportMapper = mock(ReportMapper.class);
    private final PostMapper postMapper = mock(PostMapper.class);
    private final CommentMapper commentMapper = mock(CommentMapper.class);
    private final SystemSettingService systemSettingService = mock(SystemSettingService.class);
    private final ReportService service = new ReportService(reportMapper, postMapper, commentMapper,
            systemSettingService, mock(ChatRoomGuard.class), mock(MemberService.class),
            mock(MemberSuspensionService.class));

    private void stubWriters(Long postWriter, Long commentWriter) {
        when(systemSettingService.get()).thenReturn(new SystemSetting());
        Post post = new Post();
        post.setMbrSn(postWriter);
        when(postMapper.selectPost(PST, NORMAL)).thenReturn(post);
        Comment comment = new Comment();
        comment.setMbrSn(commentWriter);
        when(commentMapper.selectComment(CMNT)).thenReturn(comment);
        doAnswer(invocation -> {
            Report arg = invocation.getArgument(0);
            arg.setDclrSn(55L);
            return 1;
        }).when(reportMapper).insertReport(any(Report.class));
    }

    private void assertSelfError(Runnable call) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.REPORT_SELF_NOT_ALLOWED));
    }

    @Test
    void 자기_글_신고는_400이고_저장하지_않는다() {
        stubWriters(ME, OTHER);

        assertSelfError(() -> service.create(ME, new ReportCreateRequest(PST, null, "CMUC0009", null)));

        verify(reportMapper, never()).insertReport(any(Report.class));
        verify(reportMapper, never()).insertStatusHistory(any());
    }

    @Test
    void 자기_댓글_신고는_400이고_저장하지_않는다() {
        stubWriters(OTHER, ME);

        assertSelfError(() -> service.create(ME, new ReportCreateRequest(null, CMNT, "CMUC0009", null)));

        verify(reportMapper, never()).insertReport(any(Report.class));
    }

    @Test
    void 자기_대상은_중복_신고_검사보다_먼저_막는다() {
        stubWriters(ME, OTHER);
        when(reportMapper.existsReport(ME, PST, null)).thenReturn(true);

        assertSelfError(() -> service.create(ME, new ReportCreateRequest(PST, null, "CMUC0009", null)));
    }

    @Test
    void 남의_글과_댓글은_그대로_접수한다() {
        stubWriters(OTHER, OTHER);

        ReportResponse post = service.create(ME, new ReportCreateRequest(PST, null, "CMUC0009", null));
        ReportResponse comment = service.create(ME, new ReportCreateRequest(null, CMNT, "CMUC0009", null));

        assertThat(post.dclrSn()).isEqualTo(55L);
        assertThat(comment.dclrSn()).isEqualTo(55L);
    }
}
