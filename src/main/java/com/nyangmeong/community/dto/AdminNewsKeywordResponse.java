package com.nyangmeong.community.dto;

import com.nyangmeong.community.domain.NewsKeyword;

/**
 * [관리자 뉴스 분류 키워드 항목]
 * - GET /api/admin/news/keywords (FN-ADM-050-05)
 * - mpngCd : 카테고리 코드(CMUG06), sortSeq : 수집 분류 때 이 순서대로 처음 걸린 카테고리
 */
public record AdminNewsKeywordResponse(Long kwrdDictSn, String mpngCd, String kwrdCn, Integer sortSeq) {

    public static AdminNewsKeywordResponse from(NewsKeyword keyword) {
        return new AdminNewsKeywordResponse(keyword.getKwrdDictSn(), keyword.getMpngCd(), keyword.getKwrdCn(),
                keyword.getSortSeq());
    }
}
