package com.nyangmeong.community.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.nyangmeong.community.domain.Post;

/**
 * [관리자 게시글 상세 응답]
 * - 댓글 목록 : GET /api/admin/comments?pstSn=
 */
public record AdminPostDetailResponse(Long pstSn, String bbsCd, String pstSttsCd, String pstTtl, String pstCn,
        Long mbrSn, String mbrNcnm, LocalDateTime regDt, int dclrCnt, int cmntCnt, List<PostImageSummary> images,
        List<AdminPostStatusHistorySummary> statusHistories) {

    public static AdminPostDetailResponse of(Post post, String mbrNcnm, int dclrCnt, int cmntCnt,
            List<PostImageSummary> images, List<AdminPostStatusHistorySummary> statusHistories) {
        return new AdminPostDetailResponse(post.getPstSn(), post.getBbsCd(), post.getPstSttsCd(), post.getPstTtl(),
                post.getPstCn(), post.getMbrSn(), mbrNcnm, post.getRegDt(), dclrCnt, cmntCnt, images,
                statusHistories);
    }
}
