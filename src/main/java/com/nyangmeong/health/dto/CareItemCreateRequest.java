package com.nyangmeong.health.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [케어 항목 추가 요청]
 * - 정기관리 주기·직접입력 일수 필수는 서비스에서 재검증
 */
public record CareItemCreateRequest(
        @NotBlank @Size(max = 40) String careArtclNm,
        @Size(max = 20) String rprsIconId,
        @NotBlank String careZoneCd,
        String careCycleCd,
        @Min(1) @Max(9999) Integer cycleDrctInptDayCnt,
        LocalDate lastCmptnYmd) {
}
