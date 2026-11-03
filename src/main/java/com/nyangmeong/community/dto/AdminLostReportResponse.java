package com.nyangmeong.community.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.nyangmeong.community.domain.LostReportDetail;

/**
 * [관리자 실종제보 상세 응답]
 * - 찾았어요 글은 제목·상태·전환 시각만 주고 나머지는 null (결정 13)
 * - 연락처·반려동물 정보는 주지 않는다
 */
public record AdminLostReportResponse(
        Long lostTpofSn,
        Long pstSn,
        String pstTtl,
        String lostTpofSttsCd,
        LocalDateTime dscvSwtcDt,
        Long mbrSn,
        String mbrNcnm,
        LocalDateTime regDt,
        String pstCn,
        LocalDateTime expryCrtrDt,
        List<PostImageSummary> imgs) {

    /**
     * 실종·만료 응답
     */
    public static AdminLostReportResponse of(LostReportDetail detail, List<PostImageSummary> imgs) {
        return new AdminLostReportResponse(detail.getLostTpofSn(), detail.getPstSn(), detail.getPstTtl(),
                detail.getLostTpofSttsCd(), detail.getDscvSwtcDt(), detail.getMbrSn(), detail.getMbrNcnm(),
                detail.getPstRegDt(), detail.getPstCn(), detail.getExpryCrtrDt(), imgs);
    }

    /**
     * 찾았어요 응답 (제목·상태·전환 시각만)
     */
    public static AdminLostReportResponse closed(LostReportDetail detail) {
        return new AdminLostReportResponse(detail.getLostTpofSn(), detail.getPstSn(), detail.getPstTtl(),
                detail.getLostTpofSttsCd(), detail.getDscvSwtcDt(), null, null, null, null, null, null);
    }
}
