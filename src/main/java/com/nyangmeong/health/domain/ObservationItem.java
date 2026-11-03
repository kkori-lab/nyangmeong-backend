package com.nyangmeong.health.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관찰항목 VO]
 * - OBSRV_ARTCL 1:1
 */
@Data
public class ObservationItem {

    /** 관찰항목일련번호 */
    private Long obsrvArtclSn;
    /** 반려동물일련번호 */
    private Long petSn;
    /** 관찰부위공통코드(HLTG16) */
    private String obsrvRegnCd;
    /** 관찰상태공통코드(HLTG18) */
    private String obsrvSttsCd;
    /** 관찰항목명 */
    private String obsrvArtclNm;
    /** 관찰부위상세명 */
    private String obsrvRegnDtlNm;
    /** 관찰종료일자 */
    private LocalDate obsrvEndYmd;
    /** 등록일시 */
    private LocalDateTime regDt;
    /** 수정일시 */
    private LocalDateTime mdfcnDt;
    /** 등록자ID */
    private String rgtrId;
    /** 수정자ID */
    private String mdfrId;
    /** 삭제여부 */
    private String delYn;
}
