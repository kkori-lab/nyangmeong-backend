package com.nyangmeong.place.dto;

import java.math.BigDecimal;

import lombok.Data;

/**
 * [장소 검색 결과 요약]
 * - distance : 검색 시점 지도 중앙에서의 거리(미터)
 * - plcBkmkSn : 내가 즐겨찾기한 업체면 값
 */
@Data
public class PlaceSummary {
    private Long plcSn;
    private String plcClsfCd;
    private String plcClsfCdNm;
    private String plcConm;
    private String plcAddr;
    private String rprsTelno;
    private BigDecimal plcLat;
    private BigDecimal plcLot;
    private String kakaoPlcId;
    private String kakaoUrlPath;
    private Integer distance;
    private Long plcBkmkSn;
}
