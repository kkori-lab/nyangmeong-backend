package com.nyangmeong.community.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 신고 목록 항목]
 */
@Data
public class AdminReportSummary {
    private Long dclrSn;
    private Long pstSn;
    private Long cmntSn;
    /** 게시글 제목 또는 댓글 내용 */
    private String trgtCn;
    private String dclrRsnCd;
    /** 신고자 */
    private Long mbrSn;
    private String mbrNcnm;
    /** 같은 대상 신고 수 */
    private int dclrCnt;
    private String dclrPrcsSttsCd;
    private String dclrActnCd;
    private LocalDateTime regDt;
}
