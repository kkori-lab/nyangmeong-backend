package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.community.domain.Report;
import com.nyangmeong.community.domain.ReportAction;
import com.nyangmeong.community.domain.ReportStatus;
import com.nyangmeong.community.domain.ReportTargetType;
import com.nyangmeong.community.dto.AdminPostStatusChangeRequest;
import com.nyangmeong.community.dto.AdminReportCompleteRequest;
import com.nyangmeong.community.dto.AdminReportDetailResponse;
import com.nyangmeong.community.dto.AdminReportSearchCond;
import com.nyangmeong.community.mapper.CommentMapper;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.community.mapper.ReportMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.member.dto.AdminMemberSuspendRequest;
import com.nyangmeong.member.service.MemberSuspensionService;

/**
 * [검사 - 관리자 채팅 상대 신고 처리]
 */
class AdminChatReportServiceTest {

    private static final Long ACTOR = 1L;
    private static final Long DCLR = 7L;
    private static final Long OTHER_DCLR = 8L;
    private static final Long ROOM = 40L;
    private static final Long TARGET = 20L;
    private static final Long REPORTER = 30L;
    private static final Long OTHER_REPORTER = 31L;
    private static final String RECEIVED = ReportStatus.RECEIVED.getCode();

    private final ReportMapper reportMapper = mock(ReportMapper.class);
    private final PostMapper postMapper = mock(PostMapper.class);
    private final CommentMapper commentMapper = mock(CommentMapper.class);
    private final AdminPostService adminPostService = mock(AdminPostService.class);
    private final AdminCommentService adminCommentService = mock(AdminCommentService.class);
    private final MemberSuspensionService memberSuspensionService = mock(MemberSuspensionService.class);
    private final AdminReportService service = new AdminReportService(reportMapper, postMapper, commentMapper,
            adminPostService, adminCommentService, memberSuspensionService, mock(AdminTaskLogService.class),
            mock(EventPublisher.class));

    private Report chatReport(Long dclrSn, Long mbrSn) {
        Report report = new Report();
        report.setDclrSn(dclrSn);
        report.setMbrSn(mbrSn);
        report.setOtoChatRoomSn(ROOM);
        report.setTrgtMbrSn(TARGET);
        report.setDclrPrcsSttsCd(RECEIVED);
        return report;
    }

    private void stubChatReports() {
        Report first = chatReport(DCLR, REPORTER);
        Report second = chatReport(OTHER_DCLR, OTHER_REPORTER);
        when(reportMapper.selectReport(DCLR)).thenReturn(first);
        when(reportMapper.selectMemberReportsByStatus(TARGET, RECEIVED)).thenReturn(List.of(first, second));
        when(reportMapper.updateProcessed(anyLong(), eq(RECEIVED), any(), any(), eq(ACTOR))).thenReturn(1);
    }

    private void assertError(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run).isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue("errorCode",
                code);
    }

    @Test
    void 정지로_처리하면_같은_회원의_접수_신고를_함께_처리하고_대상_회원을_정지한다() {
        stubChatReports();

        service.complete(ACTOR, DCLR,
                new AdminReportCompleteRequest(ReportAction.SUSPEND.getCode(), null, 7, "채팅 욕설"));

        verify(reportMapper).updateProcessed(DCLR, RECEIVED, ReportStatus.COMPLETED.getCode(),
                ReportAction.SUSPEND.getCode(), ACTOR);
        verify(reportMapper).updateProcessed(OTHER_DCLR, RECEIVED, ReportStatus.COMPLETED.getCode(),
                ReportAction.SUSPEND.getCode(), ACTOR);
        ArgumentCaptor<AdminMemberSuspendRequest> req = ArgumentCaptor.forClass(AdminMemberSuspendRequest.class);
        verify(memberSuspensionService).suspend(eq(ACTOR), eq(TARGET), req.capture());
        assertThat(req.getValue().suspnDayCnt()).isEqualTo(7);
        verify(postMapper, never()).selectAdminPost(any());
        verify(commentMapper, never()).selectAdminComment(any());
    }

    @Test
    void 유지로_처리하면_대상은_그대로다() {
        stubChatReports();

        service.complete(ACTOR, DCLR, new AdminReportCompleteRequest(ReportAction.KEEP.getCode(), null, null, null));

        verify(memberSuspensionService, never()).suspend(any(), any(), any());
        verify(adminPostService, never()).blind(any(), any(), any());
        verify(adminCommentService, never()).delete(any(), any(), any());
    }

    @Test
    void 블라인드로_처리하면_400이고_신고는_접수_그대로다() {
        stubChatReports();

        assertError(() -> service.complete(ACTOR, DCLR,
                new AdminReportCompleteRequest(ReportAction.BLIND.getCode(), null, null, null)),
                ErrorCode.INVALID_INPUT_VALUE);
        verify(reportMapper, never()).updateProcessed(anyLong(), any(), any(), any(), any());
    }

    @Test
    void 자동_블라인드_복구_대상이_아니라_409다() {
        stubChatReports();

        assertError(() -> service.restore(ACTOR, DCLR, new AdminPostStatusChangeRequest("복구")),
                ErrorCode.ALREADY_PROCESSED);
        verify(memberSuspensionService, never()).release(any(), any(), any());
    }

    @Test
    void 상세는_대상_회원과_채팅방을_주고_같은_회원의_신고를_묶는다() {
        stubChatReports();
        when(postMapper.selectMbrNcnm(TARGET)).thenReturn("상대");

        AdminReportDetailResponse res = service.get(DCLR);

        assertThat(res.trgtMbrSn()).isEqualTo(TARGET);
        assertThat(res.trgtMbrNcnm()).isEqualTo("상대");
        assertThat(res.otoChatRoomSn()).isEqualTo(ROOM);
        assertThat(res.pstSn()).isNull();
        assertThat(res.cmntSn()).isNull();
        verify(reportMapper).selectSameTargetList(null, null, TARGET);
    }

    @Test
    void 목록의_회원_대상_필터를_넘긴다() {
        AdminReportSearchCond cond = new AdminReportSearchCond();
        cond.setTargetType(ReportTargetType.MEMBER);

        service.search(cond, new PageQuery(null, null, null));

        verify(reportMapper).selectAdminList(eq(cond), eq("MEMBER"), any(), any(), any());
    }
}
