package com.nyangmeong.health.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * [케어 완료 기록 목록 행]
 * - 기록 내용 (FN-HLT-003-12)
 */
@Data
public class CareCompletionSummary {

    /** 케어완료이력일련번호 */
    private Long careCmptnHstrySn;
    /** 케어항목일련번호 */
    private Long careArtclSn;
    /** 케어항목명 */
    private String careArtclNm;
    /** 대표아이콘ID */
    private String rprsIconId;
    /** 완료일자 */
    private LocalDate cmptnYmd;
    /** 완료회차 */
    private Integer cmptnCycl;
}
