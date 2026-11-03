package com.nyangmeong.health.dto;

import lombok.Data;

/**
 * [기타검사 목록 행]
 * - 검사 결과 탭 기타검사 칩 (FN-HLT-005-02)
 */
@Data
public class EtcInspectionSummary {

    /** 기타검사일련번호 */
    private Long etcInspSn;
    /** 검진기록일련번호 */
    private Long ckupRcdSn;
    /** 검사명 */
    private String inspNm;
    /** 검사결과내용 */
    private String inspRsltCn;
    /** 메모내용 */
    private String memoCn;
}
