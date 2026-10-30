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
import com.nyangmeong.support.domain.FaqCategory;
import com.nyangmeong.support.dto.FaqSearchCond;
import com.nyangmeong.support.dto.FaqSummary;
import com.nyangmeong.support.mapper.FaqMapper;

import lombok.RequiredArgsConstructor;

/**
 * [자주 묻는 질문 서비스]
 * - SCR-CSC-002 목록, 작성은 관리자 기능이라 없음
 */
@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqMapper faqMapper;

    /**
     * FAQ 목록 조회 (카테고리·키워드는 AND)
     */
    @Transactional(readOnly = true)
    public PageResponse<FaqSummary> search(FaqSearchCond cond, PageQuery pq) {
        String faqClsfCd = trimToNull(cond.getFaqClsfCd());
        if (faqClsfCd != null && !FaqCategory.exists(faqClsfCd)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String keyword = trimToNull(cond.getKeyword());
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOr(10));
        List<FaqSummary> rows = faqMapper.selectList(faqClsfCd, keyword);
        return PageResponse.of(new PageInfo<>(rows));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
