package com.nyangmeong.community.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * [관리자 뉴스 등록 요청]
 * - POST /api/admin/news (FN-ADM-050-02), 출처는 관리자 등록(CMUC0023)
 * - newsTtl : 앞뒤 공백을 지운 뒤 1~200자, newsCn(요약) : 1~1000자
 * - atchFileSn : POST /api/admin/news/images 로 올린 썸네일, 없으면 null
 */
public record AdminNewsCreateRequest(@NotBlank String newsTtl, @NotBlank String newsCn, @NotBlank String newsClsfCd,
        Long atchFileSn) {
}
