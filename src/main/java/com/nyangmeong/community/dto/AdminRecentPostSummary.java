package com.nyangmeong.community.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 대시보드 최근 게시글 한 줄]
 * - SCR-ADM-001 (FN-ADM-001-02)
 */
@Data
public class AdminRecentPostSummary {
    /** 게시글일련번호 */
    private Long pstSn;
    /** 게시판공통코드 */
    private String bbsCd;
    /** 게시판공통코드명 */
    private String bbsNm;
    /** 게시글제목 */
    private String pstTtl;
    /** 등록일시 */
    private LocalDateTime regDt;
}
