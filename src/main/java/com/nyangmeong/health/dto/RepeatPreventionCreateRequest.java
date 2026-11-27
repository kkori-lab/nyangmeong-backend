package com.nyangmeong.health.dto;

import java.time.LocalDate;

import com.nyangmeong.global.aitool.AiCode;
import com.nyangmeong.global.aitool.AiRequiredIf;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * [반복 예방 등록 요청]
 * - 직접입력 주기의 일수 필수는 서비스에서 검증 (FN-HLT-005-05)
 */
@AiRequiredIf(field = "cycleDrctInptDayCnt", when = "reptCycleCd", equals = "HLTC0042")
public record RepeatPreventionCreateRequest(
        @NotNull Long ckupRcdSn,
        @NotBlank @AiCode("HLTG07") String reptPrvntArtclCd,
        @NotBlank @AiCode("HLTG15") String reptCycleCd,
        @Min(1) @Max(365) Integer cycleDrctInptDayCnt,
        @NotNull LocalDate bgngYmd) {
}
