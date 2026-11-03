package com.nyangmeong.support.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.support.domain.Notice;
import com.nyangmeong.support.dto.NoticeResponse;
import com.nyangmeong.support.dto.NoticeSearchCond;
import com.nyangmeong.support.dto.NoticeSummary;
import com.nyangmeong.support.mapper.NoticeMapper;

import lombok.RequiredArgsConstructor;

/**
 * [공지사항 서비스]
 * - SCR-CSC-001 목록·상세, 작성은 관리자 기능이라 없음, 조회수는 쓰지 않음
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeMapper noticeMapper;

    /**
     * 공지사항 목록 조회 (상단 고정 우선, 제목 검색)
     */
    @Transactional(readOnly = true)
    public PageResponse<NoticeSummary> search(NoticeSearchCond cond, PageQuery pq) {
        String keyword = cond.getKeyword() == null ? null : cond.getKeyword().trim();
        if (keyword != null && keyword.isEmpty()) {
            keyword = null;
        }
        boolean latest = Boolean.TRUE.equals(cond.getLatestYn());
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOr(10));
        List<NoticeSummary> rows = noticeMapper.selectList(keyword, latest);
        return PageResponse.of(new PageInfo<>(rows));
    }

    /**
     * 공지사항 상세 조회
     */
    @Transactional(readOnly = true)
    public NoticeResponse getNotice(Long ntcMttrSn) {
        Notice notice = noticeMapper.selectNotice(ntcMttrSn);
        if (notice == null) {
            throw new CustomException(ErrorCode.NOTICE_NOT_FOUND);
        }
        return NoticeResponse.from(notice);
    }
}
