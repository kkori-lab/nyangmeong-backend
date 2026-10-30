package com.nyangmeong.community.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.community.domain.Comment;
import com.nyangmeong.community.domain.LostReport;
import com.nyangmeong.community.domain.LostReportStatus;
import com.nyangmeong.community.domain.Post;
import com.nyangmeong.community.domain.PostStatus;
import com.nyangmeong.community.dto.CommentCreateRequest;
import com.nyangmeong.community.dto.CommentResponse;
import com.nyangmeong.community.dto.CommentSummary;
import com.nyangmeong.community.mapper.CommentMapper;
import com.nyangmeong.community.mapper.LostReportMapper;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.global.auth.OwnerAuthorizer;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * [댓글 서비스]
 * - SCR-CMU-001 댓글·대댓글 목록·등록·삭제, 실종센터 상세(SCR-CMU-002)도 같이 사용
 * - 대댓글에 단 답글은 최상위 댓글에 붙여 1depth 유지
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final LostReportMapper lostReportMapper;
    private final OwnerAuthorizer ownerAuthorizer;

    /**
     * 댓글·대댓글 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CommentSummary> search(Long mbrSn, Long pstSn) {
        Post post = checkPost(pstSn);
        /** 찾았어요 처리된 실종 글의 댓글은 작성자만 본다 */
        LostReport lostReport = lostReportMapper.selectByPstSn(pstSn);
        if (isFound(lostReport) && !mbrSn.equals(post.getMbrSn())) {
            return List.of();
        }
        return commentMapper.selectList(pstSn, mbrSn);
    }

    /**
     * 댓글·대댓글 등록
     */
    @Transactional
    public CommentResponse create(Long mbrSn, Long pstSn, CommentCreateRequest req) {
        checkPost(pstSn);
        if (isFound(lostReportMapper.selectByPstSn(pstSn))) {
            throw new CustomException(ErrorCode.LOST_REPORT_CLOSED);
        }
        Long prntCmntSn = null;
        if (req.prntCmntSn() != null) {
            Comment parent = commentMapper.selectComment(req.prntCmntSn());
            if (parent == null || !pstSn.equals(parent.getPstSn())) {
                throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
            }
            /** 대댓글에 단 답글은 원 댓글 스레드에 이어서 노출 */
            prntCmntSn = parent.getPrntCmntSn() != null ? parent.getPrntCmntSn() : parent.getCmntSn();
        }

        Comment comment = new Comment();
        comment.setPrntCmntSn(prntCmntSn);
        comment.setPstSn(pstSn);
        comment.setMbrSn(mbrSn);
        comment.setCmntCn(req.cmntCn());
        commentMapper.insertComment(comment);
        return CommentResponse.from(commentMapper.selectComment(comment.getCmntSn()),
                postMapper.selectMbrNcnm(mbrSn));
    }

    /**
     * 댓글 삭제 (답글은 유지)
     */
    @Transactional
    public void delete(Long mbrSn, Long pstSn, Long cmntSn) {
        Comment comment = commentMapper.selectComment(cmntSn);
        if (comment == null || !pstSn.equals(comment.getPstSn())) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }
        ownerAuthorizer.checkOwner(mbrSn, comment.getMbrSn());
        commentMapper.updateDeleted(cmntSn);
    }

    /**
     * 정상 상태 게시글 확인
     */
    private Post checkPost(Long pstSn) {
        Post post = postMapper.selectPost(pstSn, PostStatus.NORMAL.getCode());
        if (post == null) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }
        return post;
    }

    private boolean isFound(LostReport lostReport) {
        return lostReport != null && LostReportStatus.FOUND.is(lostReport.getLostTpofSttsCd());
    }
}
