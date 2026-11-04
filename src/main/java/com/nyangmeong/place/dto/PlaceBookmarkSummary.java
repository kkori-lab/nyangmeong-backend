package com.nyangmeong.place.dto;

import java.math.BigDecimal;

import lombok.Data;

/**
 * [장소 즐겨찾기 목록 요약]
 */
@Data
public class PlaceBookmarkSummary {
    private Long plcBkmkSn;
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
}
