package com.nyangmeong.health.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * [반복 예방 등록 요청]
 * - 직접입력 주기의 일수 필수는 서비스에서 검증 (FN-HLT-005-05)
 */
public record RepeatPreventionCreateRequest(
        @NotNull Long ckupRcdSn,
        @NotBlank String reptPrvntArtclCd,
        @NotBlank String reptCycleCd,
        @Min(1) @Max(9999) Integer cycleDrctInptDayCnt,
        @NotNull LocalDate bgngYmd) {
}
