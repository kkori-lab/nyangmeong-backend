package com.nyangmeong.health.dto;

import com.nyangmeong.global.aitool.AiCode;
import com.nyangmeong.global.aitool.AiRequiredIf;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * [케어 항목 수정 요청]
 * - 기본 항목은 정기관리의 주기·직접입력 일수만 변경
 */
@AiRequiredIf(field = "cycleDrctInptDayCnt", when = "careCycleCd", equals = "HLTC0042")
public record CareItemUpdateRequest(
        @Size(max = 40) String careArtclNm,
        @Size(max = 20) String rprsIconId,
        @AiCode("HLTG13") String careZoneCd,
        @AiCode("HLTG15") String careCycleCd,
        @Min(1) @Max(365) Integer cycleDrctInptDayCnt) {
}
