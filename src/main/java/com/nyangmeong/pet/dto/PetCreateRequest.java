package com.nyangmeong.pet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.nyangmeong.global.aitool.AiCode;
import com.nyangmeong.global.aitool.AiOneOf;
import com.nyangmeong.global.aitool.AiRequiredIf;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * [반려동물 등록 요청]
 * - 품종은 vrtySn·vrtyDrctInptNm 중 하나, 털색 직접 입력이면 colrDrctInptNm 필수 (서비스에서 검사)
 * - petBdwg·goalBdwg·minBdwg·maxBdwg : 소수 둘째 자리까지, 200kg 이하
 * - bdwgRngDrctInptYn : 'Y' 직접 입력(minBdwg·maxBdwg 필수), 'N' 자동 계산(두 값은 무시), 안 보내면 'Y'
 * - @AiOneOf·@AiRequiredIf : AI 매니저 도구 목록에 같은 규칙 표시
 */
@AiOneOf({"vrtySn", "vrtyDrctInptNm"})
@AiRequiredIf(field = "colrDrctInptNm", when = "colrCd", equals = "PETC0017")
@AiRequiredIf(field = "minBdwg", when = "bdwgRngDrctInptYn", equals = "Y")
@AiRequiredIf(field = "maxBdwg", when = "bdwgRngDrctInptYn", equals = "Y")
public record PetCreateRequest(@NotBlank @Size(max = 20) String petNm,
        @NotBlank @AiCode("PETG01") String petSeCd,
        Long vrtySn,
        @Size(max = 40) String vrtyDrctInptNm,
        @NotNull LocalDate petBrdt,
        @Pattern(regexp = "^([0-9]{12}|[0-9]{15})?$") String chipNo,
        @NotBlank @AiCode("PETG02") String petGndrCd,
        @NotNull Boolean nutrYn,
        @NotNull @Positive @Digits(integer = 3, fraction = 2) @DecimalMax("200.00") BigDecimal petBdwg,
        @Positive @Digits(integer = 3, fraction = 2) @DecimalMax("200.00") BigDecimal goalBdwg,
        @Positive @Digits(integer = 3, fraction = 2) @DecimalMax("200.00") BigDecimal minBdwg,
        @Positive @Digits(integer = 3, fraction = 2) @DecimalMax("200.00") BigDecimal maxBdwg,
        @Pattern(regexp = "^[YN]$") String bdwgRngDrctInptYn,
        @NotBlank @AiCode("PETG03") String colrCd,
        @Size(max = 20) String colrDrctInptNm,
        @Size(max = 1000) String chrcCn,
        @Size(max = 1000) String algyCn,
        @Size(max = 1000) String dssCn) {
}
