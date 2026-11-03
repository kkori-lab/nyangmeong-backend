package com.nyangmeong.health.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * [처방약 수정 요청]
 * - 보낸 항목만 수정 : 수정 결과 종료일 ≥ 시작일 (서비스에서 검증)
 */
public record PrescriptionUpdateRequest(
        @Size(max = 40) String mdcnNm,
        LocalDate mdcnBgngYmd,
        LocalDate mdcnEndYmd,
        @Min(1) @Max(9999) Integer dayMdcnNmtm,
        String prscSttsCd,
        Boolean careRfltYn) {
}
