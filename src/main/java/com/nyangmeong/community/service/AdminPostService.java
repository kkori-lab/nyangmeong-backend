package com.nyangmeong.community.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.community.domain.BoardType;
import com.nyangmeong.community.domain.Post;
import com.nyangmeong.community.domain.PostStatus;
import com.nyangmeong.community.domain.PostStatusHistory;
import com.nyangmeong.community.dto.AdminBoardPostCountSummary;
import com.nyangmeong.community.dto.AdminDashboardPostResponse;
import com.nyangmeong.community.dto.AdminPostDetailResponse;
import com.nyangmeong.community.dto.AdminPostSearchCond;
import com.nyangmeong.community.dto.AdminPostStatusChangeRequest;
import com.nyangmeong.community.dto.AdminPostSummary;
import com.nyangmeong.community.dto.PostImageSummary;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.community.mapper.ReportMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.log.AdminTaskLogService;
import com.nyangmeong.global.log.AdminTaskType;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 게시글 서비스]
 * - SCR-ADM-020 목록·검색·상세·블라인드·복구
 */
@Service
@RequiredArgsConstructor
public class AdminPostService {

    /** 게시판 공통코드 그룹 */
    private static final String BOARD_GROUP_CD = "CMUG01";

    private static final String TARGET_TABLE = "PST";
    private static final int DASHBOARD_RECENT_LIMIT = 5;

    private final PostMapper postMapper;
    private final ReportMapper reportMapper;
    private final AdminTaskLogService adminTaskLogService;

    /** 감사 로그 전후 값 */
    private record PostState(String pstSttsCd, String chgRsn) {
    }

    /**
     * 게시글 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminPostSummary> search(AdminPostSearchCond cond, PageQuery pq) {
        if (cond.getBgngYmd() != null && cond.getEndYmd() != null && cond.getBgngYmd().isAfter(cond.getEndYmd())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (hasText(cond.getBbsCd())
                && Arrays.stream(BoardType.values()).noneMatch(type -> type.is(cond.getBbsCd()))) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (hasText(cond.getStatus())
                && Arrays.stream(PostStatus.values()).noneMatch(status -> status.is(cond.getStatus()))) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String keyword = hasText(cond.getQ()) ? escapeLike(cond.getQ().strip()) : null;

        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOrDefault());
        List<AdminPostSummary> rows = postMapper.selectAdminList(cond, keyword);
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 게시글 상세 조회 (상태 무관)
     */
    @Transactional(readOnly = true)
    public AdminPostDetailResponse get(Long pstSn) {
        Post post = getOrThrow(pstSn);
        return AdminPostDetailResponse.of(post, postMapper.selectMbrNcnm(post.getMbrSn()),
                reportMapper.countReports(pstSn, null), postMapper.countComments(pstSn),
                postMapper.selectImages(pstSn).stream().map(PostImageSummary::from).toList(),
                postMapper.selectAdminStatusHistories(pstSn));
    }

    /**
     * 게시글 블라인드
     */
    @Transactional
    public void blind(Long actorSn, Long pstSn, AdminPostStatusChangeRequest req) {
        changeStatus(actorSn, pstSn, PostStatus.NORMAL, PostStatus.BLIND, req.chgRsn(), "게시글 블라인드");
    }

    /**
     * 블라인드 복구 (신고 누적 자동 블라인드 포함)
     */
    @Transactional
    public void restore(Long actorSn, Long pstSn, AdminPostStatusChangeRequest req) {
        changeStatus(actorSn, pstSn, PostStatus.BLIND, PostStatus.NORMAL, req.chgRsn(), "게시글 블라인드 복구");
    }

    private void changeStatus(Long actorSn, Long pstSn, PostStatus before, PostStatus after, String chgRsn,
            String summary) {
        Post post = getOrThrow(pstSn);
        if (!before.is(post.getPstSttsCd())) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        if (postMapper.updateStatus(pstSn, before.getCode(), after.getCode(), String.valueOf(actorSn)) != 1) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }
        PostStatusHistory history = new PostStatusHistory();
        history.setPstSn(pstSn);
        history.setMbrSn(actorSn);
        history.setBfrSttsCd(before.getCode());
        history.setAftrSttsCd(after.getCode());
        history.setChgRsn(chgRsn);
        postMapper.insertStatusHistory(history);
        adminTaskLogService.record(actorSn, AdminTaskType.PROCESS, TARGET_TABLE, pstSn, summary,
                new PostState(before.getCode(), null), new PostState(after.getCode(), chgRsn));
    }

    private Post getOrThrow(Long pstSn) {
        Post post = postMapper.selectAdminPost(pstSn);
        if (post == null) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }
        return post;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * LIKE 와일드카드 문자 이스케이프
     */
    private String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * 관리자 대시보드 게시글 현황 조회
     */
    @Transactional(readOnly = true)
    public AdminDashboardPostResponse getDashboardSummary() {
        return new AdminDashboardPostResponse(postMapper.countAll(),
                postMapper.selectRecentPosts(DASHBOARD_RECENT_LIMIT));
    }

    /**
     * 관리자 대시보드 게시판별 게시글 수 조회 (fromDt 이상 toDt 미만)
     */
    @Transactional(readOnly = true)
    public List<AdminBoardPostCountSummary> getBoardPostCounts(LocalDateTime fromDt, LocalDateTime toDt) {
        return postMapper.selectBoardPostCounts(BOARD_GROUP_CD, fromDt, toDt);
    }
}
