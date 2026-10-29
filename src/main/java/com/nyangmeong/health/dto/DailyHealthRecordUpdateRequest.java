package com.nyangmeong.health.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

/**
 * [일상 건강기록 수정 요청]
 * - 보낸 값만 수정 (FN-HLT-002-13 인라인 편집)
 */
public record DailyHealthRecordUpdateRequest(
        LocalDateTime rcdDt,
        @Digits(integer = 5, fraction = 3) BigDecimal rcdNvl,
        @Size(max = 8) String crsdSttsCd) {
}
