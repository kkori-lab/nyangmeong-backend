package com.nyangmeong.community.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.community.domain.Comment;
import com.nyangmeong.community.domain.PostStatus;
import com.nyangmeong.community.domain.PostStatusHistory;
import com.nyangmeong.community.domain.Report;
import com.nyangmeong.community.domain.ReportReason;
import com.nyangmeong.community.domain.ReportStatus;
import com.nyangmeong.community.domain.ReportStatusHistory;
import com.nyangmeong.community.dto.ReportCreateRequest;
import com.nyangmeong.community.dto.ReportResponse;
import com.nyangmeong.community.mapper.CommentMapper;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.community.mapper.ReportMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.setting.SystemSettingService;

import lombok.RequiredArgsConstructor;

/**
 * [게시글·댓글 신고 서비스]
 * - SCR-CMU-001 신고 접수, 실종센터·펫돌봄 상세도 같이 사용
 * - 누적 신고가 기준(SYS_STNG.BLIND_DCLR_ACML_CNT)에 닿으면 글은 블라인드, 댓글은 삭제 처리
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    /** PST_STTS_HSTRY.CHG_RSN : 처리자 없는 이 사유가 자동 블라인드 표시 */
    public static final String AUTO_BLIND_RSN = "신고 누적";
    /** DCLR_STTS_HSTRY.CHG_RSN : CMNT 에 구분 컬럼이 없어 이 이력으로 자동 삭제 판별 */
    public static final String AUTO_DELETE_RSN = "신고 누적 자동 삭제";

    private final ReportMapper reportMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final SystemSettingService systemSettingService;

    /**
     * 신고 접수
     */
    @Transactional
    public ReportResponse create(Long mbrSn, ReportCreateRequest req) {
        if (req.pstSn() == null == (req.cmntSn() == null) || !ReportReason.exists(req.dclrRsnCd())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (ReportReason.ETC.is(req.dclrRsnCd()) && (req.dclrDtlCn() == null || req.dclrDtlCn().isBlank())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        boolean isPost = req.pstSn() != null;
        if (isPost) {
            if (postMapper.selectPost(req.pstSn(), PostStatus.NORMAL.getCode()) == null) {
                throw new CustomException(ErrorCode.POST_NOT_FOUND);
            }
        } else if (commentMapper.selectComment(req.cmntSn()) == null) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }
        ErrorCode duplicate = isPost ? ErrorCode.REPORT_ALREADY_EXISTS : ErrorCode.REPORT_COMMENT_ALREADY_EXISTS;
        if (reportMapper.existsReport(mbrSn, req.pstSn(), req.cmntSn())) {
            throw new CustomException(duplicate);
        }

        Report report = new Report();
        report.setMbrSn(mbrSn);
        report.setPstSn(req.pstSn());
        report.setCmntSn(req.cmntSn());
        report.setDclrRsnCd(req.dclrRsnCd());
        report.setDclrPrcsSttsCd(ReportStatus.RECEIVED.getCode());
        report.setDclrDtlCn(req.dclrDtlCn());
        try {
            reportMapper.insertReport(report);
        } catch (DuplicateKeyException e) {
            /** 동시에 같은 대상을 신고한 경우 */
            throw new CustomException(duplicate);
        }
        ReportStatusHistory history = new ReportStatusHistory();
        history.setDclrSn(report.getDclrSn());
        history.setMbrSn(mbrSn);
        history.setAftrSttsCd(ReportStatus.RECEIVED.getCode());
        reportMapper.insertStatusHistory(history);

        applyAccumulation(isPost, req, report.getDclrSn());
        return ReportResponse.from(report);
    }

    /**
     * 누적 신고 기준 도달 시 자동 처리
     * - 반려 제외 : 복구한 대상이 새 신고 1건에 다시 숨겨지지 않게
     */
    private void applyAccumulation(boolean isPost, ReportCreateRequest req, Long dclrSn) {
        long limit = systemSettingService.get().getBlindDclrAcmlCnt();
        if (reportMapper.countAccumulatedReports(req.pstSn(), req.cmntSn(),
                ReportStatus.REJECTED.getCode()) < limit) {
            return;
        }
        if (!isPost) {
            if (commentMapper.updateDeleted(req.cmntSn()) > 0) {
                ReportStatusHistory history = new ReportStatusHistory();
                history.setDclrSn(dclrSn);
                history.setBfrSttsCd(ReportStatus.RECEIVED.getCode());
                history.setAftrSttsCd(ReportStatus.RECEIVED.getCode());
                history.setChgRsn(AUTO_DELETE_RSN);
                reportMapper.insertStatusHistory(history);
            }
            return;
        }
        if (postMapper.updateStatus(req.pstSn(), PostStatus.NORMAL.getCode(), PostStatus.BLIND.getCode(),
                "SYSTEM") > 0) {
            PostStatusHistory history = new PostStatusHistory();
            history.setPstSn(req.pstSn());
            history.setBfrSttsCd(PostStatus.NORMAL.getCode());
            history.setAftrSttsCd(PostStatus.BLIND.getCode());
            history.setChgRsn(AUTO_BLIND_RSN);
            postMapper.insertStatusHistory(history);
        }
    }
}
