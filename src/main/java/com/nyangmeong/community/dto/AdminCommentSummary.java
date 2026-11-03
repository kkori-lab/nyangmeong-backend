package com.nyangmeong.community.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 댓글 목록 항목]
 * - 삭제된 댓글도 내용·작성자 표시
 */
@Data
public class AdminCommentSummary {
    private Long cmntSn;
    private Long prntCmntSn;
    private Long pstSn;
    private String pstTtl;
    private Long mbrSn;
    private String mbrNcnm;
    private String cmntCn;
    private boolean delYn;
    private boolean sysAutoCrtYn;
    private LocalDateTime regDt;
}
