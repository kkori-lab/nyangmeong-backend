package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.community.domain.Comment;
import com.nyangmeong.community.domain.Post;
import com.nyangmeong.community.domain.PostStatus;
import com.nyangmeong.community.domain.Report;
import com.nyangmeong.community.domain.ReportAction;
import com.nyangmeong.community.domain.ReportStatus;
import com.nyangmeong.community.domain.ReportStatusHistory;
import com.nyangmeong.community.dto.AdminPostStatusChangeRequest;
import com.nyangmeong.community.dto.AdminReportCompleteRequest;
import com.nyangmeong.community.dto.AdminReportRejectRequest;
import com.nyangmeong.community.dto.AdminReportSearchCond;
import com.nyangmeong.community.mapper.CommentMapper;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.community.mapper.ReportMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.notification.NotificationEvent;
import com.nyangmeong.global.notification.NotificationType;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.member.dto.AdminMemberSuspendRequest;
import com.nyangmeong.member.service.MemberSuspensionService;

/**
 * [검사 - 관리자 신고 처리]
 */
class AdminReportServiceTest {

    private static final Long ACTOR = 1L;
    private static final Long DCLR = 7L;
    private static final Long OTHER_DCLR = 8L;
    private static final Long PST = 3L;
    private static final Long CMNT = 9L;
    private static final Long AUTHOR = 20L;
    private static final Long REPORTER = 30L;
    private static final Long OTHER_REPORTER = 31L;
    private static final String RECEIVED = ReportStatus.RECEIVED.getCode();

    private final ReportMapper reportMapper = mock(ReportMapper.class);
    private final PostMapper postMapper = mock(PostMapper.class);
    private final CommentMapper commentMapper = mock(CommentMapper.class);
    private final AdminPostService adminPostService = mock(AdminPostService.class);
    private final AdminCommentService adminCommentService = mock(AdminCommentService.class);
    private final MemberSuspensionService memberSuspensionService = mock(MemberSuspensionService.class);
    private final AdminTaskLogService adminTaskLogService = mock(AdminTaskLogService.class);
    private final EventPublisher eventPublisher = mock(EventPublisher.class);
    private final AdminReportService service = new AdminReportService(reportMapper, postMapper, commentMapper,
            adminPostService, adminCommentService, memberSuspensionService, adminTaskLogService, eventPublisher);

    private Report report(Long dclrSn, Long mbrSn, Long pstSn, Long cmntSn) {
        Report report = new Report();
        report.setDclrSn(dclrSn);
        report.setMbrSn(mbrSn);
        report.setPstSn(pstSn);
        report.setCmntSn(cmntSn);
        report.setDclrPrcsSttsCd(RECEIVED);
        return report;
    }

    private void stubPostReports() {
        Report first = report(DCLR, REPORTER, PST, null);
        Report second = report(OTHER_DCLR, OTHER_REPORTER, PST, null);
        when(reportMapper.selectReport(DCLR)).thenReturn(first);
        when(reportMapper.selectReportsByStatus(PST, null, RECEIVED)).thenReturn(List.of(first, second));
        when(reportMapper.updateProcessed(anyLong(), eq(RECEIVED), any(), any(), eq(ACTOR))).thenReturn(1);
        Post post = new Post();
        post.setPstSn(PST);
        post.setMbrSn(AUTHOR);
        post.setPstSttsCd(PostStatus.NORMAL.getCode());
        when(postMapper.selectAdminPost(PST)).thenReturn(post);
    }

    private void assertError(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run).isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue("errorCode",
                code);
    }

    @Test
    void 코드가_틀리거나_기간이_뒤집히면_목록은_400() {
        AdminReportSearchCond wrongStatus = new AdminReportSearchCond();
        wrongStatus.setStatus("WRONG");
        AdminReportSearchCond wrongReason = new AdminReportSearchCond();
        wrongReason.setDclrRsnCd("WRONG");
        AdminReportSearchCond wrongPeriod = new AdminReportSearchCond();
        wrongPeriod.setBgngYmd(LocalDate.of(2026, 11, 2));
        wrongPeriod.setEndYmd(LocalDate.of(2026, 11, 1));
        PageQuery pq = new PageQuery(null, null, null);

        assertError(() -> service.search(wrongStatus, pq), ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.search(wrongReason, pq), ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.search(wrongPeriod, pq), ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 없는_신고는_404() {
        assertError(() -> service.get(DCLR), ErrorCode.REPORT_NOT_FOUND);
    }

    @Test
    void 처리_완료는_같은_대상의_접수_신고를_함께_처리하고_신고자마다_알린다() {
        stubPostReports();

        service.complete(ACTOR, DCLR,
                new AdminReportCompleteRequest(ReportAction.KEEP.getCode(), "문제 없음", null, null));

        verify(reportMapper).updateProcessed(DCLR, RECEIVED, ReportStatus.COMPLETED.getCode(),
                ReportAction.KEEP.getCode(), ACTOR);
        verify(reportMapper).updateProcessed(OTHER_DCLR, RECEIVED, ReportStatus.COMPLETED.getCode(),
                ReportAction.KEEP.getCode(), ACTOR);
        verify(reportMapper, times(2)).insertStatusHistory(any(ReportStatusHistory.class));
        ArgumentCaptor<NotificationEvent> event = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher, times(2)).publish(event.capture());
        assertThat(event.getAllValues()).extracting(NotificationEvent::mbrSn)
                .containsExactly(REPORTER, OTHER_REPORTER);
        assertThat(event.getAllValues()).allMatch(e -> e.type() == NotificationType.REPORT_RESULT);
        verify(adminPostService, never()).blind(any(), any(), any());
        verify(memberSuspensionService, never()).suspend(any(), any(), any());
    }

    @Test
    void 조치가_블라인드면_글을_블라인드한다() {
        stubPostReports();

        service.complete(ACTOR, DCLR,
                new AdminReportCompleteRequest(ReportAction.BLIND.getCode(), null, null, null));

        verify(adminPostService).blind(eq(ACTOR), eq(PST), any(AdminPostStatusChangeRequest.class));
    }

    @Test
    void 이미_블라인드된_글은_신고만_처리한다() {
        stubPostReports();
        Post post = new Post();
        post.setPstSttsCd(PostStatus.BLIND.getCode());
        when(postMapper.selectAdminPost(PST)).thenReturn(post);

        service.complete(ACTOR, DCLR,
                new AdminReportCompleteRequest(ReportAction.BLIND.getCode(), null, null, null));

        verify(adminPostService, never()).blind(any(), any(), any());
        verify(reportMapper, times(2)).insertStatusHistory(any(ReportStatusHistory.class));
    }

    @Test
    void 조치가_블라인드인_댓글은_삭제한다() {
        Report report = report(DCLR, REPORTER, null, CMNT);
        when(reportMapper.selectReport(DCLR)).thenReturn(report);
        when(reportMapper.selectReportsByStatus(null, CMNT, RECEIVED)).thenReturn(List.of(report));
        when(reportMapper.updateProcessed(anyLong(), eq(RECEIVED), any(), any(), eq(ACTOR))).thenReturn(1);
        Comment comment = new Comment();
        comment.setDelYn("N");
        when(commentMapper.selectAdminComment(CMNT)).thenReturn(comment);

        service.complete(ACTOR, DCLR,
                new AdminReportCompleteRequest(ReportAction.BLIND.getCode(), null, null, null));

        verify(adminCommentService).delete(eq(ACTOR), eq(CMNT), any());
    }

    @Test
    void 조치가_작성자_정지면_같은_규칙으로_정지한다() {
        stubPostReports();

        service.complete(ACTOR, DCLR,
                new AdminReportCompleteRequest(ReportAction.SUSPEND.getCode(), null, 7, "반복 광고"));

        ArgumentCaptor<AdminMemberSuspendRequest> req = ArgumentCaptor.forClass(AdminMemberSuspendRequest.class);
        verify(memberSuspensionService).suspend(eq(ACTOR), eq(AUTHOR), req.capture());
        assertThat(req.getValue().suspnDayCnt()).isEqualTo(7);
        assertThat(req.getValue().suspnRsn()).isEqualTo("반복 광고");
    }

    @Test
    void 정지_사유가_없거나_조치_코드가_틀리면_400() {
        assertError(() -> service.complete(ACTOR, DCLR,
                new AdminReportCompleteRequest(ReportAction.SUSPEND.getCode(), null, 7, " ")),
                ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.complete(ACTOR, DCLR, new AdminReportCompleteRequest("WRONG", null, null, null)),
                ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 정지할_수_없는_작성자면_예외가_그대로_나간다() {
        stubPostReports();
        doThrow(new CustomException(ErrorCode.MEMBER_NOT_SUSPENDABLE)).when(memberSuspensionService).suspend(any(),
                any(), any());

        assertError(() -> service.complete(ACTOR, DCLR,
                new AdminReportCompleteRequest(ReportAction.SUSPEND.getCode(), null, null, "사유")),
                ErrorCode.MEMBER_NOT_SUSPENDABLE);
    }

    @Test
    void 이미_처리된_신고는_처리도_반려도_409() {
        Report report = report(DCLR, REPORTER, PST, null);
        report.setDclrPrcsSttsCd(ReportStatus.COMPLETED.getCode());
        when(reportMapper.selectReport(DCLR)).thenReturn(report);

        assertError(() -> service.complete(ACTOR, DCLR,
                new AdminReportCompleteRequest(ReportAction.KEEP.getCode(), null, null, null)),
                ErrorCode.ALREADY_PROCESSED);
        assertError(() -> service.reject(ACTOR, DCLR, new AdminReportRejectRequest("사유")),
                ErrorCode.ALREADY_PROCESSED);
    }

    @Test
    void 동시에_처리되면_409이고_알리지_않는다() {
        stubPostReports();
        when(reportMapper.updateProcessed(anyLong(), eq(RECEIVED), any(), any(), eq(ACTOR))).thenReturn(0);

        assertError(() -> service.complete(ACTOR, DCLR,
                new AdminReportCompleteRequest(ReportAction.BLIND.getCode(), null, null, null)),
                ErrorCode.ALREADY_PROCESSED);

        verify(eventPublisher, never()).publish(any(NotificationEvent.class));
        verify(adminPostService, never()).blind(any(), any(), any());
    }

    @Test
    void 반려는_그_신고_1건만_처리하고_사유를_이력에_남긴다() {
        stubPostReports();

        service.reject(ACTOR, DCLR, new AdminReportRejectRequest("근거 없음"));

        verify(reportMapper).updateProcessed(DCLR, RECEIVED, ReportStatus.REJECTED.getCode(), null, ACTOR);
        verify(reportMapper, never()).updateProcessed(eq(OTHER_DCLR), any(), any(), any(), any());
        ArgumentCaptor<ReportStatusHistory> history = ArgumentCaptor.forClass(ReportStatusHistory.class);
        verify(reportMapper).insertStatusHistory(history.capture());
        assertThat(history.getValue().getChgRsn()).isEqualTo("근거 없음");
        verify(eventPublisher).publish(any(NotificationEvent.class));
    }

    @Test
    void 자동_블라인드_글을_복구하면_접수_신고를_반려하고_알린다() {
        stubPostReports();
        when(reportMapper.existsAutoTarget(eq(DCLR), any(), any(), any())).thenReturn(true);
        AdminPostStatusChangeRequest req = new AdminPostStatusChangeRequest("오신고");

        service.restore(ACTOR, DCLR, req);

        verify(adminPostService).restore(ACTOR, PST, req);
        verify(reportMapper).updateProcessed(OTHER_DCLR, RECEIVED, ReportStatus.REJECTED.getCode(), null, ACTOR);
        verify(eventPublisher, times(2)).publish(any(NotificationEvent.class));
    }

    @Test
    void 자동_삭제_댓글을_복구하면_삭제_여부를_되돌린다() {
        Report report = report(DCLR, REPORTER, null, CMNT);
        when(reportMapper.selectReport(DCLR)).thenReturn(report);
        when(reportMapper.existsAutoTarget(eq(DCLR), any(), any(), any())).thenReturn(true);
        when(reportMapper.selectReportsByStatus(null, CMNT, RECEIVED)).thenReturn(List.of(report));
        when(reportMapper.updateProcessed(anyLong(), eq(RECEIVED), any(), any(), eq(ACTOR))).thenReturn(1);
        when(commentMapper.updateRestored(CMNT)).thenReturn(1);

        service.restore(ACTOR, DCLR, new AdminPostStatusChangeRequest("오신고"));

        verify(commentMapper).updateRestored(CMNT);
        verify(eventPublisher).publish(any(NotificationEvent.class));
    }

    @Test
    void 자동_대상이_아니면_복구는_409() {
        when(reportMapper.selectReport(DCLR)).thenReturn(report(DCLR, REPORTER, PST, null));

        assertError(() -> service.restore(ACTOR, DCLR, new AdminPostStatusChangeRequest("오신고")),
                ErrorCode.ALREADY_PROCESSED);

        verify(adminPostService, never()).restore(any(), any(), any());
    }
}
