package com.nyangmeong.community.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.nyangmeong.community.domain.Comment;
import com.nyangmeong.community.domain.Post;
import com.nyangmeong.community.domain.Report;

/**
 * [관리자 신고 상세 응답]
 * - 대상이 게시글이면 pstTtl·pstCn·pstSttsCd, 댓글이면 cmntCn·delYn
 */
public record AdminReportDetailResponse(Long dclrSn, String dclrRsnCd, String dclrDtlCn, String dclrPrcsSttsCd,
        String dclrActnCd, Long mbrSn, String mbrNcnm, LocalDateTime prcsDt, LocalDateTime regDt, Long pstSn,
        Long cmntSn, String pstTtl, String pstCn, String pstSttsCd, String cmntCn, Boolean delYn, Long trgtMbrSn,
        String trgtMbrNcnm, List<AdminReportSummary> sameTargetReports,
        List<AdminReportStatusHistorySummary> statusHistories) {

    public static AdminReportDetailResponse of(Report report, String mbrNcnm, Post post, Comment comment,
            String trgtMbrNcnm, List<AdminReportSummary> sameTargetReports,
            List<AdminReportStatusHistorySummary> statusHistories) {
        return new AdminReportDetailResponse(report.getDclrSn(), report.getDclrRsnCd(), report.getDclrDtlCn(),
                report.getDclrPrcsSttsCd(), report.getDclrActnCd(), report.getMbrSn(), mbrNcnm, report.getPrcsDt(),
                report.getRegDt(), report.getPstSn(), report.getCmntSn(), post != null ? post.getPstTtl() : null,
                post != null ? post.getPstCn() : null, post != null ? post.getPstSttsCd() : null,
                comment != null ? comment.getCmntCn() : null, comment != null ? "Y".equals(comment.getDelYn()) : null,
                post != null ? post.getMbrSn() : comment.getMbrSn(), trgtMbrNcnm, sameTargetReports, statusHistories);
    }
}
