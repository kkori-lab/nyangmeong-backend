package com.nyangmeong.place.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [장소 VO]
 * - PLC 1:1
 */
@Data
public class Place {
    /** 장소일련번호 */
    private Long plcSn;
    /** 지역일련번호 */
    private Long rgnSn;
    /** 장소카테고리공통코드 */
    private String plcClsfCd;
    /** 장소출처공통코드 */
    private String plcSrcCd;
    /** 장소상호명 */
    private String plcConm;
    /** 장소주소 */
    private String plcAddr;
    /** 대표전화번호 */
    private String rprsTelno;
    /** 카카오장소ID */
    private String kakaoPlcId;
    /** 공공데이터키 */
    private String pblDataKey;
    /** 카카오URL경로 */
    private String kakaoUrlPath;
    /** 장소위도 */
    private BigDecimal plcLat;
    /** 장소경도 */
    private BigDecimal plcLot;
    /** 사용여부 */
    private String useYn;
    /** 등록일시 */
    private LocalDateTime regDt;
    /** 수정일시 */
    private LocalDateTime mdfcnDt;
    /** 등록자ID */
    private String rgtrId;
    /** 수정자ID */
    private String mdfrId;
}
