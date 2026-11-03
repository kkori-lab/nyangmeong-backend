package com.nyangmeong.health.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관찰회차 VO]
 * - OBSRV_CYCL 1:1 (회차 번호는 저장하지 않고 조회 때 관찰일 순으로 매긴다)
 */
@Data
public class ObservationCycle {

    /** 관찰회차일련번호 */
    private Long obsrvCyclSn;
    /** 관찰항목일련번호 */
    private Long obsrvArtclSn;
    /** 첨부파일일련번호 */
    private Long atchFileSn;
    /** 관찰변화공통코드(HLTG17) */
    private String obsrvChgCd;
    /** 관찰크기(mm) */
    private BigDecimal obsrvSz;
    /** 메모내용 */
    private String memoCn;
    /** 관찰일시 */
    private LocalDateTime obsrvDt;
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
