package com.nyangmeong.health.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [검사 수치 VO]
 * - INSP_NVL 1:1 : 직접입력이 아니면 항목명은 빈 문자열
 */
@Data
public class InspectionValue {

    /** 검사수치일련번호 */
    private Long inspNvlSn;
    /** 검진기록일련번호 */
    private Long ckupRcdSn;
    /** 검사항목공통코드(HLTG12) */
    private String inspArtclCd;
    /** 검사항목직접입력명 */
    private String inspArtclDrctInptNm;
    /** 단위명 */
    private String unitNm;
    /** 결과수치 */
    private BigDecimal rsltNvl;
    /** 참고하한수치 */
    private BigDecimal refLlmtNvl;
    /** 참고상한수치 */
    private BigDecimal refUplmtNvl;
    /** 메모내용 */
    private String memoCn;
    /** 정렬순서 */
    private Integer sortSeq;
    /** 삭제여부 */
    private String delYn;
    /** 등록일시 */
    private LocalDateTime regDt;
    /** 수정일시 */
    private LocalDateTime mdfcnDt;
    /** 등록자ID */
    private String rgtrId;
    /** 수정자ID */
    private String mdfrId;
}
