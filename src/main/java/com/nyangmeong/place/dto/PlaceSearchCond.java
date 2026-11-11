package com.nyangmeong.place.dto;

import java.math.BigDecimal;

import com.nyangmeong.global.aitool.AiCode;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * [장소 검색 조건]
 * - plcClsfCd : 카테고리(PLCG01) 필수, keyword : 지역·상호명·키워드 100자 이하
 * - sw·ne : 검색 범위(지도 영역)의 남서·북동 모서리, cntr : 거리 기준(검색 실행 시점의 지도 중앙)
 * - 좌표 6칸은 모두 보내거나 모두 생략 (서비스 검사). 생략하면 전국 정확도순·distance 없음 (AI 대화창, 2026-11-11)
 * - 정렬·쪽은 PageQuery 의 sort(RECOMMEND·DISTANCE·NAME)·page, 쪽 크기는 15 고정
 */
@Data
public class PlaceSearchCond {
    @NotBlank
    @AiCode("PLCG01")
    private String plcClsfCd;
    @Size(max = 100)
    private String keyword;
    @DecimalMin("-90") @DecimalMax("90")
    private BigDecimal swLat;
    @DecimalMin("-180") @DecimalMax("180")
    private BigDecimal swLot;
    @DecimalMin("-90") @DecimalMax("90")
    private BigDecimal neLat;
    @DecimalMin("-180") @DecimalMax("180")
    private BigDecimal neLot;
    @DecimalMin("-90") @DecimalMax("90")
    private BigDecimal cntrLat;
    @DecimalMin("-180") @DecimalMax("180")
    private BigDecimal cntrLot;

    /**
     * 좌표 6칸이 모두 있는지
     */
    public boolean hasCoordinates() {
        return swLat != null && swLot != null && neLat != null && neLot != null && cntrLat != null
                && cntrLot != null;
    }

    /**
     * 좌표 6칸이 모두 없는지
     */
    public boolean hasNoCoordinates() {
        return swLat == null && swLot == null && neLat == null && neLot == null && cntrLat == null
                && cntrLot == null;
    }
}
