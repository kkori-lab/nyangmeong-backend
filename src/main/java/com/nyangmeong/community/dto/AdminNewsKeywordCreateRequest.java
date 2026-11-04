package com.nyangmeong.community.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * [관리자 뉴스 분류 키워드 등록 요청]
 * - POST /api/admin/news/keywords (FN-ADM-050-05)
 * - mpngCd : 카테고리 코드(CMUG06 4종), kwrdCn : 앞뒤 공백을 지운 뒤 1~50자, 쉼표 불가
 */
public record AdminNewsKeywordCreateRequest(@NotBlank String mpngCd, @NotBlank String kwrdCn) {
}
