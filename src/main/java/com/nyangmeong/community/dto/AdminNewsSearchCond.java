package com.nyangmeong.community.dto;

import lombok.Data;

/**
 * [관리자 뉴스 검색 조건]
 * - GET /api/admin/news (FN-ADM-050-01)
 * - newsSrcCd : 출처 코드(CMUG07), 없거나 공백이면 전체
 * - newsClsfCd : 카테고리 코드(CMUG06), 없거나 공백이면 전체
 */
@Data
public class AdminNewsSearchCond {
    private String newsSrcCd;
    private String newsClsfCd;
}
