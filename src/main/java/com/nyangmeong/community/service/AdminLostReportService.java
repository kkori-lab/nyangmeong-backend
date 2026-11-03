package com.nyangmeong.community.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.community.domain.LostReportDetail;
import com.nyangmeong.community.domain.LostReportStatus;
import com.nyangmeong.community.domain.PostStatus;
import com.nyangmeong.community.dto.AdminLostReportResponse;
import com.nyangmeong.community.dto.AdminLostReportSearchCond;
import com.nyangmeong.community.dto.AdminLostReportSummary;
import com.nyangmeong.community.dto.PostImageSummary;
import com.nyangmeong.community.mapper.LostReportMapper;
import com.nyangmeong.community.mapper.PostMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;

import lombok.RequiredArgsConstructor;

/**
 * [관리자 실종제보 서비스]
 * - SCR-ADM-021 목록·상세 (강제 만료·기간 조정 없음, 결정 12)
 */
@Service
@RequiredArgsConstructor
public class AdminLostReportService {

    private final LostReportMapper lostReportMapper;
    private final PostMapper postMapper;

    /**
     * 목록 조회 (정상 게시글만, 최신순)
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminLostReportSummary> search(AdminLostReportSearchCond cond, PageQuery pq) {
        String raw = cond.getLostTpofSttsCd();
        String sttsCd = raw == null || raw.isBlank() ? null : raw;
        if (sttsCd != null && Arrays.stream(LostReportStatus.values()).noneMatch(status -> status.is(sttsCd))) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOrDefault());
        List<AdminLostReportSummary> rows = lostReportMapper.selectAdminList(sttsCd, PostStatus.NORMAL.getCode(),
                LostReportStatus.MISSING.getCode(), LostReportStatus.FOUND.getCode(),
                LostReportStatus.EXPIRED.getCode());
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 상세 조회 (찾았어요는 제목·상태·처리 시각만, 조회수는 올리지 않음)
     */
    @Transactional(readOnly = true)
    public AdminLostReportResponse get(Long lostTpofSn) {
        LostReportDetail detail = lostReportMapper.selectDetail(lostTpofSn, PostStatus.NORMAL.getCode());
        if (detail == null) {
            throw new CustomException(ErrorCode.LOST_REPORT_NOT_FOUND);
        }
        if (LostReportStatus.FOUND.is(detail.getLostTpofSttsCd())) {
            return AdminLostReportResponse.closed(detail);
        }
        /** 만료 배치가 없어 DB에는 실종으로 남으므로 만료 상태로 내린다 */
        if (LostReportStatus.MISSING.is(detail.getLostTpofSttsCd())
                && detail.getExpryCrtrDt().isBefore(LocalDateTime.now())) {
            detail.setLostTpofSttsCd(LostReportStatus.EXPIRED.getCode());
        }
        List<PostImageSummary> imgs = postMapper.selectImages(detail.getPstSn()).stream()
                .map(PostImageSummary::from).toList();
        return AdminLostReportResponse.of(detail, imgs);
    }
}
