package com.nyangmeong.health.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

/**
 * [검사 수치 수정 요청]
 * - 보낸 항목만 수정, 검사 항목은 변경 불가 : 단위는 빈 값 불가, 참고범위는 수정 결과로 짝 검증 (서비스에서 검증)
 */
public record InspectionValueUpdateRequest(
        @Digits(integer = 11, fraction = 4) BigDecimal rsltNvl,
        @Size(max = 20) String unitNm,
        @Digits(integer = 11, fraction = 4) BigDecimal refLlmtNvl,
        @Digits(integer = 11, fraction = 4) BigDecimal refUplmtNvl,
        @Size(max = 200) String memoCn) {
}
