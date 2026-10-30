package com.nyangmeong.health.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [일상 건강기록 등록 요청]
 * - 배변상태면 crsdSttsCd 필수, 그 밖이면 rcdNvl 필수 (FN-HLT-002-11)
 */
public record DailyHealthRecordCreateRequest(
        @NotBlank @Size(max = 8) String dailyRcdIdctCd,
        @NotNull LocalDateTime rcdDt,
        @Digits(integer = 5, fraction = 3) BigDecimal rcdNvl,
        @Size(max = 8) String crsdSttsCd) {
}
