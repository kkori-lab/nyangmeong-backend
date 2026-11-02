package com.nyangmeong.global.external;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * [외부 API 호출 통지 요청]
 * - callDt 가 없으면 저장 시각
 */
public record ExternalApiCallRequest(
        @NotBlank String extlApiSeCd,
        @NotBlank String callRsltCd,
        LocalDateTime callDt,
        @PositiveOrZero @Max(9999999999L) Long rspnsMs,
        @Min(100) @Max(599) Integer httpSttsVl,
        @PositiveOrZero @Max(9999999999L) Long inptTknCnt,
        @PositiveOrZero @Max(9999999999L) Long otptTknCnt,
        @Size(max = 1000) String failRsn) {
}
