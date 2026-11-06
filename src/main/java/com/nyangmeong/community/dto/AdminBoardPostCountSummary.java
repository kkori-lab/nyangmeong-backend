package com.nyangmeong.community.dto;

import lombok.Data;

/**
 * [관리자 대시보드 게시판별 게시글 수]
 * - SCR-ADM-001 (FN-ADM-001-04) : 게시판 전부, 글이 없으면 0
 */
@Data
public class AdminBoardPostCountSummary {
    /** 게시판공통코드(CMUG01) */
    private String bbsCd;
    /** 게시판 이름 */
    private String bbsNm;
    /** 게시글 수 */
    private long cnt;
}
