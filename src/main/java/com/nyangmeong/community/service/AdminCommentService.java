package com.nyangmeong.community.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.community.domain.Comment;
import com.nyangmeong.community.dto.AdminCommentDeleteRequest;
import com.nyangmeong.community.dto.AdminCommentSearchCond;
import com.nyangmeong.community.dto.AdminCommentSummary;
import com.nyangmeong.community.mapper.CommentMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 댓글 서비스]
 * - SCR-ADM-020 (FN-ADM-020-06) 조회·삭제
 * - CMNT 에 사유 컬럼이 없어 삭제 사유는 감사 로그에 남긴다
 */
@Service
@RequiredArgsConstructor
public class AdminCommentService {

    private static final String TARGET_TABLE = "CMNT";
    private static final String DELETED = "Y";

    private final CommentMapper commentMapper;
    private final AdminTaskLogService adminTaskLogService;

    /** 감사 로그 전후 값 */
    private record CommentState(String delYn, String delRsn) {
    }

    /**
     * 댓글 목록 조회 (삭제된 댓글 포함)
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminCommentSummary> search(AdminCommentSearchCond cond, PageQuery pq) {
        String q = cond.getQ();
        String keyword = q != null && !q.isBlank() ? escapeLike(q.strip()) : null;

        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOrDefault());
        List<AdminCommentSummary> rows = commentMapper.selectAdminList(cond, keyword);
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void delete(Long actorSn, Long cmntSn, AdminCommentDeleteRequest req) {
        Comment comment = commentMapper.selectAdminComment(cmntSn);
        if (comment == null) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }
        if (DELETED.equals(comment.getDelYn()) || commentMapper.updateDeleted(cmntSn) != 1) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        adminTaskLogService.record(actorSn, AdminTaskType.DELETE, TARGET_TABLE, cmntSn, "댓글 삭제",
                new CommentState("N", null), new CommentState(DELETED, req.delRsn()));
    }

    /**
     * LIKE 와일드카드 문자 이스케이프
     */
    private String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
