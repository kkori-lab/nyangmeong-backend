package com.nyangmeong.health.dto;

/**
 * [관찰 항목 등록 응답]
 * - 항목과 함께 만든 1회차 번호 (FN-HLT-002-25)
 */
public record ObservationItemResponse(
        Long obsrvArtclSn,
        String obsrvArtclNm,
        String obsrvRegnCd,
        String obsrvRegnDtlNm,
        String obsrvSttsCd,
        Long obsrvCyclSn) {
}
