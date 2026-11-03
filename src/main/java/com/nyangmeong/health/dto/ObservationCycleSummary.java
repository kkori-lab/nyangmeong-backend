package com.nyangmeong.health.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관찰 회차 목록 행]
 * - 필름스트립·크기 그래프·선택 회차 상세, 항목 요약의 첫·최근 회차 (FN-HLT-002-15·17~19)
 */
@Data
public class ObservationCycleSummary {

    /** 관찰회차일련번호 */
    private Long obsrvCyclSn;
    /** 관찰항목일련번호 */
    private Long obsrvArtclSn;
    /** 관찰일 순 회차 번호 */
    private Integer cyclNo;
    /** 관찰일시 */
    private LocalDateTime obsrvDt;
    /** 관찰크기(mm) */
    private BigDecimal obsrvSz;
    /** 관찰변화공통코드(HLTG17) */
    private String obsrvChgCd;
    /** 관찰변화공통코드명 */
    private String obsrvChgCdNm;
    /** 메모내용 */
    private String memoCn;
}
