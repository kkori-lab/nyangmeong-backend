package com.nyangmeong.health.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [처방약 추가 요청]
 * - careRfltYn 미전송은 주기 체크 OFF (issues 13 가정)
 */
public record PrescriptionCreateRequest(
        @NotNull Long ckupRcdSn,
        @NotBlank @Size(max = 40) String mdcnNm,
        @NotNull LocalDate mdcnBgngYmd,
        @NotNull LocalDate mdcnEndYmd,
        @NotNull @Min(1) @Max(9999) Integer dayMdcnNmtm,
        @NotBlank String prscSttsCd,
        Boolean careRfltYn) {
}
