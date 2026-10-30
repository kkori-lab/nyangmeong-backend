package com.nyangmeong.health.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [검사 수치 목록 행]
 * - 검진상세 혈액검사 결과 (FN-HLT-005-02)
 */
@Data
public class InspectionValueSummary {

    /** 검사수치일련번호 */
    private Long inspNvlSn;
    /** 검진기록일련번호 */
    private Long ckupRcdSn;
    /** 검진일 */
    private LocalDateTime schdlDt;
    /** 검사항목공통코드(HLTG12) */
    private String inspArtclCd;
    /** 검사항목명(약어), 직접입력 항목은 null */
    private String inspArtclCdNm;
    /** 검사항목직접입력명 */
    private String inspArtclDrctInptNm;
    /** 결과수치 */
    private BigDecimal rsltNvl;
    /** 단위명 */
    private String unitNm;
    /** 참고하한수치 */
    private BigDecimal refLlmtNvl;
    /** 참고상한수치 */
    private BigDecimal refUplmtNvl;
    /** 메모내용 */
    private String memoCn;
    /** 정렬순서 */
    private Integer sortSeq;
}
