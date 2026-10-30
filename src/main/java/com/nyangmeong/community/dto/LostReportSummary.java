package com.nyangmeong.community.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [실종 제보 목록 항목]
 * - 찾았어요 상태 카드는 작성자가 아니면 제목·상태·전환 시각만 채운다(FN-CMU-002-09)
 */
@Data
public class LostReportSummary {
    private Long lostTpofSn;
    private String pstTtl;
    private String lostTpofSttsCd;
    private LocalDateTime dscvSwtcDt;
    private String petSeCd;
    private String petNm;
    private String vrtyNm;
    private String rgnNm;
    private String colrNm;
    private String chrcCn;
    private Long thumbAtchFileSn;
    private LocalDateTime regDt;
}
