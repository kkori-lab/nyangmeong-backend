package com.nyangmeong.community.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 게시글 목록 항목]
 */
@Data
public class AdminPostSummary {
    private Long pstSn;
    private String bbsCd;
    private String pstTtl;
    private Long mbrSn;
    private String mbrNcnm;
    private String pstSttsCd;
    private int dclrCnt;
    private LocalDateTime regDt;
}
