package com.nyangmeong.support.dto;

import lombok.Data;

/**
 * [관리자 공지 검색 조건]
 * - keyword : 제목 부분 일치, 없거나 공백이면 전체 (FN-ADM-040-01)
 */
@Data
public class AdminNoticeSearchCond {
    private String keyword;
}
