package com.nyangmeong.pet.dto;

import java.util.List;

import com.nyangmeong.global.response.PageResponse;

/**
 * [관리자 캐릭터 생성 현황 응답]
 * - 상태별 건수와 목록 쪽 : { sttsCnts, content, page, size, totalElements, totalPages, hasNext }
 */
public record AdminCharacterJobListResponse(
        List<AdminCharacterJobStatusCount> sttsCnts,
        List<AdminCharacterJobRow> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext) {

    public static AdminCharacterJobListResponse of(List<AdminCharacterJobStatusCount> sttsCnts,
            PageResponse<AdminCharacterJobRow> page) {
        return new AdminCharacterJobListResponse(sttsCnts, page.content(), page.page(), page.size(),
                page.totalElements(), page.totalPages(), page.hasNext());
    }
}
