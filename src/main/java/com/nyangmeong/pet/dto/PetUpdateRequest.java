package com.nyangmeong.pet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.nyangmeong.global.aitool.AiCode;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * [반려동물 수정 요청]
 * - 보낸 필드만 수정, 문자열 "" -> 비움 (docs/convention/api.md)
 * - bdwgRngDrctInptYn 을 'N'으로 보내면 현재 체중으로 범위를 다시 계산한다 (minBdwg·maxBdwg 는 무시)
 * - petBdwg·goalBdwg·minBdwg·maxBdwg : 소수 둘째 자리까지, 200kg 이하
 * - goalBdwgReset 이 true 면 목표체중을 비운다, goalBdwg 와 함께 보내면 400
 */
public record PetUpdateRequest(@Size(min = 1, max = 20) String petNm,
        @Size(min = 1) @AiCode("PETG01") String petSeCd,
        Long vrtySn,
        @Size(max = 40) String vrtyDrctInptNm,
        LocalDate petBrdt,
        @Pattern(regexp = "^[0-9]{0,20}$") String chipNo,
        @Size(min = 1) @AiCode("PETG02") String petGndrCd,
        Boolean nutrYn,
        @Positive @Digits(integer = 3, fraction = 2) @DecimalMax("200.00") BigDecimal petBdwg,
        @Positive @Digits(integer = 3, fraction = 2) @DecimalMax("200.00") BigDecimal goalBdwg,
        @Positive @Digits(integer = 3, fraction = 2) @DecimalMax("200.00") BigDecimal minBdwg,
        @Positive @Digits(integer = 3, fraction = 2) @DecimalMax("200.00") BigDecimal maxBdwg,
        @Pattern(regexp = "^[YN]$") String bdwgRngDrctInptYn,
        @Size(min = 1) @AiCode("PETG03") String colrCd,
        @Size(max = 20) String colrDrctInptNm,
        @Size(max = 1000) String chrcCn,
        @Size(max = 1000) String algyCn,
        @Size(max = 1000) String dssCn,
        Boolean goalBdwgReset) {
}
