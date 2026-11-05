package com.nyangmeong.expense.dto;

import java.util.List;

import com.nyangmeong.global.response.PageResponse;

/**
 * [관리자 영수증 OCR 현황 응답]
 * - 건수 묶음과 목록 쪽 : { summary, content, page, size, totalElements, totalPages, hasNext }
 */
public record AdminReceiptListResponse(
        AdminReceiptSummary summary,
        List<AdminReceiptRow> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext) {

    public static AdminReceiptListResponse of(AdminReceiptSummary summary, PageResponse<AdminReceiptRow> page) {
        return new AdminReceiptListResponse(summary, page.content(), page.page(), page.size(), page.totalElements(),
                page.totalPages(), page.hasNext());
    }
}
