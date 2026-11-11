package com.nyangmeong.health.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.nyangmeong.global.aitool.AiCode;
import com.nyangmeong.global.aitool.AiRequiredIf;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [일상 건강기록 등록 요청]
 * - 배변상태면 crsdSttsCd 필수, 그 밖이면 rcdNvl 필수 (FN-HLT-002-11)
 */
@AiRequiredIf(field = "crsdSttsCd", when = "dailyRcdIdctCd", equals = "HLTC0017")
@AiRequiredIf(field = "rcdNvl", when = "dailyRcdIdctCd", equals = "HLTC0014")
@AiRequiredIf(field = "rcdNvl", when = "dailyRcdIdctCd", equals = "HLTC0015")
@AiRequiredIf(field = "rcdNvl", when = "dailyRcdIdctCd", equals = "HLTC0016")
@AiRequiredIf(field = "rcdNvl", when = "dailyRcdIdctCd", equals = "HLTC0018")
public record DailyHealthRecordCreateRequest(
        @NotBlank @Size(max = 8) @AiCode("HLTG09") String dailyRcdIdctCd,
        @NotNull LocalDateTime rcdDt,
        @Digits(integer = 5, fraction = 2) BigDecimal rcdNvl,
        @Size(max = 8) @AiCode("HLTG10") String crsdSttsCd) {
}
