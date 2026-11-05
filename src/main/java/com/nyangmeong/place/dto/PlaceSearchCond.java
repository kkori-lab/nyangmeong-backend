package com.nyangmeong.place.dto;

import java.math.BigDecimal;

import com.nyangmeong.global.aitool.AiCode;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * [장소 검색 조건]
 * - plcClsfCd : 카테고리(PLCG01) 필수, keyword : 지역·상호명·키워드 100자 이하
 * - sw·ne : 검색 범위(지도 영역)의 남서·북동 모서리, cntr : 거리 기준(검색 실행 시점의 지도 중앙)
 * - 정렬·쪽은 PageQuery 의 sort(RECOMMEND·DISTANCE·NAME)·page, 쪽 크기는 15 고정
 */
@Data
public class PlaceSearchCond {
    @NotBlank
    @AiCode("PLCG01")
    private String plcClsfCd;
    @Size(max = 100)
    private String keyword;
    @NotNull @DecimalMin("-90") @DecimalMax("90")
    private BigDecimal swLat;
    @NotNull @DecimalMin("-180") @DecimalMax("180")
    private BigDecimal swLot;
    @NotNull @DecimalMin("-90") @DecimalMax("90")
    private BigDecimal neLat;
    @NotNull @DecimalMin("-180") @DecimalMax("180")
    private BigDecimal neLot;
    @NotNull @DecimalMin("-90") @DecimalMax("90")
    private BigDecimal cntrLat;
    @NotNull @DecimalMin("-180") @DecimalMax("180")
    private BigDecimal cntrLot;
}
