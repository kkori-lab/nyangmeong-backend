package com.nyangmeong.community.dto;

import java.time.LocalDateTime;

import com.nyangmeong.community.domain.Comment;

/**
 * [댓글 등록 응답]
 */
public record CommentResponse(
        Long cmntSn,
        Long pstSn,
        Long prntCmntSn,
        String cmntCn,
        Long mbrSn,
        String mbrNcnm,
        Long mbrAtchFileSn,
        LocalDateTime regDt) {

    public static CommentResponse from(Comment comment, PostAuthor author) {
        return new CommentResponse(comment.getCmntSn(), comment.getPstSn(), comment.getPrntCmntSn(),
                comment.getCmntCn(), comment.getMbrSn(), author.getMbrNcnm(), author.getMbrAtchFileSn(),
                comment.getRegDt());
    }
}
