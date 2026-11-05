package com.nyangmeong.global.external;

import java.util.List;

import com.nyangmeong.global.response.PageResponse;

/**
 * [관리자 검사결과지 분석 현황 응답]
 * - 건수 묶음과 실패 목록 쪽 : { summary, content, page, size, totalElements, totalPages, hasNext }
 */
public record PdfAnalysisListResponse(
        PdfAnalysisSummary summary,
        List<PdfAnalysisFailureRow> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext) {

    public static PdfAnalysisListResponse of(PdfAnalysisSummary summary, PageResponse<PdfAnalysisFailureRow> page) {
        return new PdfAnalysisListResponse(summary, page.content(), page.page(), page.size(), page.totalElements(),
                page.totalPages(), page.hasNext());
    }
}
