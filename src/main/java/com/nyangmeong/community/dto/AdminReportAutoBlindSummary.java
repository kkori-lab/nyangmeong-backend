package com.nyangmeong.community.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [자동 블라인드 대상 항목]
 * - 대상별 1행, dclrSn : 복구 호출에 쓰는 대상의 최신 신고
 */
@Data
public class AdminReportAutoBlindSummary {
    private Long dclrSn;
    private Long pstSn;
    private Long cmntSn;
    /** 게시글 제목 또는 댓글 내용 */
    private String trgtCn;
    /** 작성자 */
    private Long trgtMbrSn;
    private String trgtMbrNcnm;
    /** 같은 대상 신고 수 */
    private int dclrCnt;
    /** 그중 접수 상태 신고 수 */
    private int rcptDclrCnt;
    /** 마지막 신고 일시 */
    private LocalDateTime lastRegDt;
}
