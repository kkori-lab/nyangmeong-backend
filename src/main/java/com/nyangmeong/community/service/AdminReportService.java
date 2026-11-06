package com.nyangmeong.community.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.community.domain.Comment;
import com.nyangmeong.community.domain.Post;
import com.nyangmeong.community.domain.PostStatus;
import com.nyangmeong.community.domain.Report;
import com.nyangmeong.community.domain.ReportAction;
import com.nyangmeong.community.domain.ReportReason;
import com.nyangmeong.community.domain.ReportStatus;
import com.nyangmeong.community.domain.ReportStatusHistory;
import com.nyangmeong.community.domain.ReportTargetType;
import com.nyangmeong.community.dto.AdminCommentDeleteRequest;
import com.nyangmeong.community.dto.AdminPostStatusChangeRequest;
import com.nyangmeong.community.dto.AdminReportAutoBlindSummary;
import com.nyangmeong.community.dto.AdminReportCompleteRequest;
import com.nyangmeong.community.dto.AdminReportDetailResponse;
import com.nyangmeong.community.dto.AdminReportRejectRequest;
import com.nyangmeong.community.dto.AdminReportSearchCond;
import com.nyangmeong.community.dto.AdminReportSummary;
import com.nyangmeong.community.mapper.CommentMapper;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.community.mapper.ReportMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.notification.NotificationEvent;
import com.nyangmeong.global.notification.NotificationType;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.member.dto.AdminMemberSuspendRequest;
import com.nyangmeong.member.service.MemberSuspensionService;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 신고 서비스]
 * - SCR-ADM-030 목록·상세·처리 완료·반려·자동 블라인드 복구
 * - 처리 완료·복구 : 같은 대상의 접수 신고 전체, 반려 : 신고 1건
 */
@Service
@RequiredArgsConstructor
public class AdminReportService {

    private static final String TARGET_TABLE = "DCLR";
    private static final String COMMENT_TABLE = "CMNT";
    private static final String DEFAULT_ACTION_RSN = "신고 처리";
    private static final String NOTI_TTL = "신고 처리 결과";
    private static final String COMPLETED_NOTI_CN = "신고하신 내용이 처리되었어요.";
    private static final String REJECTED_NOTI_CN = "신고하신 내용이 반려되었어요.";

    private final ReportMapper reportMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final AdminPostService adminPostService;
    private final AdminCommentService adminCommentService;
    private final MemberSuspensionService memberSuspensionService;
    private final AdminTaskLogService adminTaskLogService;
    private final EventPublisher eventPublisher;

    /** 감사 로그 전후 값 */
    private record ReportState(String dclrPrcsSttsCd, String dclrActnCd, String chgRsn, int prcsCnt) {
    }

    /** 감사 로그 전후 값 */
    private record CommentState(String delYn, String chgRsn) {
    }

    /**
     * 신고 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminReportSummary> search(AdminReportSearchCond cond, PageQuery pq) {
        if (cond.getBgngYmd() != null && cond.getEndYmd() != null && cond.getBgngYmd().isAfter(cond.getEndYmd())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (hasText(cond.getStatus())
                && Arrays.stream(ReportStatus.values()).noneMatch(status -> status.is(cond.getStatus()))) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (hasText(cond.getDclrRsnCd()) && !ReportReason.exists(cond.getDclrRsnCd())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Boolean postTarget = cond.getTargetType() != null ? cond.getTargetType() == ReportTargetType.POST : null;

        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOrDefault());
        List<AdminReportSummary> rows = reportMapper.selectAdminList(cond, postTarget, PostStatus.BLIND.getCode(),
                ReportService.AUTO_BLIND_RSN, ReportService.AUTO_DELETE_RSN);
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 자동 블라인드 대상 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminReportAutoBlindSummary> searchAutoBlind(PageQuery pq) {
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOrDefault());
        List<AdminReportAutoBlindSummary> rows = reportMapper.selectAutoBlindList(ReportStatus.RECEIVED.getCode(),
                PostStatus.BLIND.getCode(), ReportService.AUTO_BLIND_RSN, ReportService.AUTO_DELETE_RSN);
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 신고 상세 조회
     */
    @Transactional(readOnly = true)
    public AdminReportDetailResponse get(Long dclrSn) {
        Report report = getOrThrow(dclrSn);
        Post post = report.getPstSn() != null ? getPost(report.getPstSn()) : null;
        Comment comment = report.getCmntSn() != null ? getComment(report.getCmntSn()) : null;
        Long trgtMbrSn = post != null ? post.getMbrSn() : comment.getMbrSn();
        return AdminReportDetailResponse.of(report, postMapper.selectMbrNcnm(report.getMbrSn()), post, comment,
                postMapper.selectMbrNcnm(trgtMbrSn),
                reportMapper.selectSameTargetList(report.getPstSn(), report.getCmntSn()),
                reportMapper.selectAdminStatusHistories(dclrSn));
    }

    /**
     * 신고 처리 완료
     */
    @Transactional
    public void complete(Long actorSn, Long dclrSn, AdminReportCompleteRequest req) {
        if (!ReportAction.exists(req.dclrActnCd())
                || (ReportAction.SUSPEND.is(req.dclrActnCd()) && !hasText(req.suspnRsn()))) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Report report = getReceived(dclrSn);
        int prcsCnt = process(actorSn, selectReceived(report), ReportStatus.COMPLETED, req.dclrActnCd(),
                req.prcsMemo(), COMPLETED_NOTI_CN);
        if (prcsCnt == 0) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        applyAction(actorSn, report, req);
        adminTaskLogService.record(actorSn, AdminTaskType.PROCESS, TARGET_TABLE, dclrSn, "신고 처리 완료",
                new ReportState(ReportStatus.RECEIVED.getCode(), null, null, 0),
                new ReportState(ReportStatus.COMPLETED.getCode(), req.dclrActnCd(), req.prcsMemo(), prcsCnt));
    }

    /**
     * 신고 반려
     */
    @Transactional
    public void reject(Long actorSn, Long dclrSn, AdminReportRejectRequest req) {
        Report report = getReceived(dclrSn);
        if (process(actorSn, List.of(report), ReportStatus.REJECTED, null, req.rjctRsn(), REJECTED_NOTI_CN) == 0) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        adminTaskLogService.record(actorSn, AdminTaskType.PROCESS, TARGET_TABLE, dclrSn, "신고 반려",
                new ReportState(ReportStatus.RECEIVED.getCode(), null, null, 0),
                new ReportState(ReportStatus.REJECTED.getCode(), null, req.rjctRsn(), 1));
    }

    /**
     * 자동 블라인드 대상 복구
     */
    @Transactional
    public void restore(Long actorSn, Long dclrSn, AdminPostStatusChangeRequest req) {
        Report report = getOrThrow(dclrSn);
        if (!reportMapper.existsAutoTarget(dclrSn, PostStatus.BLIND.getCode(), ReportService.AUTO_BLIND_RSN,
                ReportService.AUTO_DELETE_RSN)) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        if (report.getPstSn() != null) {
            adminPostService.restore(actorSn, report.getPstSn(), req);
        } else {
            restoreComment(actorSn, report.getCmntSn(), req.chgRsn());
        }
        int prcsCnt = process(actorSn, selectReceived(report), ReportStatus.REJECTED, null, req.chgRsn(),
                REJECTED_NOTI_CN);
        adminTaskLogService.record(actorSn, AdminTaskType.PROCESS, TARGET_TABLE, dclrSn, "자동 블라인드 복구",
                new ReportState(ReportStatus.RECEIVED.getCode(), null, null, 0),
                new ReportState(ReportStatus.REJECTED.getCode(), null, req.chgRsn(), prcsCnt));
    }

    private void restoreComment(Long actorSn, Long cmntSn, String chgRsn) {
        if (commentMapper.updateRestored(cmntSn) != 1) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        adminTaskLogService.record(actorSn, AdminTaskType.PROCESS, COMMENT_TABLE, cmntSn, "자동 삭제 댓글 복구",
                new CommentState("Y", null), new CommentState("N", chgRsn));
    }

    private void applyAction(Long actorSn, Report report, AdminReportCompleteRequest req) {
        if (ReportAction.BLIND.is(req.dclrActnCd())) {
            hideTarget(actorSn, report, hasText(req.prcsMemo()) ? req.prcsMemo() : DEFAULT_ACTION_RSN);
        } else if (ReportAction.SUSPEND.is(req.dclrActnCd())) {
            Long trgtMbrSn = report.getPstSn() != null ? getPost(report.getPstSn()).getMbrSn()
                    : getComment(report.getCmntSn()).getMbrSn();
            memberSuspensionService.suspend(actorSn, trgtMbrSn,
                    new AdminMemberSuspendRequest(req.suspnDayCnt(), req.suspnRsn()));
        }
    }

    /**
     * 대상 숨김 (이미 숨겨진 대상은 그대로)
     */
    private void hideTarget(Long actorSn, Report report, String chgRsn) {
        if (report.getPstSn() != null) {
            if (PostStatus.NORMAL.is(getPost(report.getPstSn()).getPstSttsCd())) {
                adminPostService.blind(actorSn, report.getPstSn(), new AdminPostStatusChangeRequest(chgRsn));
            }
            return;
        }
        if ("N".equals(getComment(report.getCmntSn()).getDelYn())) {
            adminCommentService.delete(actorSn, report.getCmntSn(), new AdminCommentDeleteRequest(chgRsn));
        }
    }

    /**
     * 신고 상태 변경·이력·신고자 알림
     */
    private int process(Long actorSn, List<Report> reports, ReportStatus after, String dclrActnCd, String chgRsn,
            String notiCn) {
        List<Report> processed = new ArrayList<>();
        for (Report report : reports) {
            /** 동시에 처리된 신고 제외 */
            if (reportMapper.updateProcessed(report.getDclrSn(), ReportStatus.RECEIVED.getCode(), after.getCode(),
                    dclrActnCd, actorSn) != 1) {
                continue;
            }
            ReportStatusHistory history = new ReportStatusHistory();
            history.setDclrSn(report.getDclrSn());
            history.setMbrSn(actorSn);
            history.setBfrSttsCd(ReportStatus.RECEIVED.getCode());
            history.setAftrSttsCd(after.getCode());
            history.setChgRsn(chgRsn);
            reportMapper.insertStatusHistory(history);
            processed.add(report);
        }
        processed.forEach(report -> eventPublisher.publish(
                new NotificationEvent(report.getMbrSn(), NotificationType.REPORT_RESULT, NOTI_TTL, notiCn, null)));
        return processed.size();
    }

    private List<Report> selectReceived(Report report) {
        return reportMapper.selectReportsByStatus(report.getPstSn(), report.getCmntSn(),
                ReportStatus.RECEIVED.getCode());
    }

    private Report getReceived(Long dclrSn) {
        Report report = getOrThrow(dclrSn);
        if (!ReportStatus.RECEIVED.is(report.getDclrPrcsSttsCd())) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        return report;
    }

    private Report getOrThrow(Long dclrSn) {
        Report report = reportMapper.selectReport(dclrSn);
        if (report == null) {
            throw new CustomException(ErrorCode.REPORT_NOT_FOUND);
        }
        return report;
    }

    private Post getPost(Long pstSn) {
        Post post = postMapper.selectAdminPost(pstSn);
        if (post == null) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }
        return post;
    }

    private Comment getComment(Long cmntSn) {
        Comment comment = commentMapper.selectAdminComment(cmntSn);
        if (comment == null) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }
        return comment;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 접수 상태 신고 수 조회
     */
    @Transactional(readOnly = true)
    public long countReceived() {
        return reportMapper.countByStatus(ReportStatus.RECEIVED.getCode());
    }
}
