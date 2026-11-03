package com.nyangmeong.global.external;

import java.math.BigDecimal;

/**
 * [장소 검색 결과 한 건]
 * - 카카오 로컬 키워드 검색 문서에서 PLC 에 쓰는 값만 옮긴 구조
 * - categoryName : 카카오 카테고리 이름(반려동물 업체 거르기용)
 */
public record PlaceSearchItem(
        String kakaoPlcId,
        String plcConm,
        String plcAddr,
        String rprsTelno,
        String kakaoUrlPath,
        BigDecimal plcLat,
        BigDecimal plcLot,
        String categoryName) {
}
