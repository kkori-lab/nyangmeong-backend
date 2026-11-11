package com.nyangmeong.health.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

/**
 * [검사 수치 수정 요청]
 * - 보낸 항목만 수정, 검사 항목은 변경 불가 : 단위는 빈 값 불가
 * - 참고범위는 refRange 묶음 : 안 보내면 유지, 보내면 두 값으로 덮어씀({null, null}이면 비움)
 */
public record InspectionValueUpdateRequest(
        @DecimalMin("0") @Digits(integer = 7, fraction = 2) BigDecimal rsltNvl,
        @Size(max = 20) String unitNm,
        @Valid RefRange refRange,
        @Size(max = 200) String memoCn) {

    /**
     * [참고범위 묶음]
     * - 둘 다 값 또는 둘 다 null (DB 짝 제약과 같음), 하한 ≤ 상한
     */
    public record RefRange(
            @DecimalMin("0") @Digits(integer = 7, fraction = 2) BigDecimal refLlmtNvl,
            @DecimalMin("0") @Digits(integer = 7, fraction = 2) BigDecimal refUplmtNvl) {

        @JsonIgnore
        @AssertTrue(message = "참고범위 하한·상한은 둘 다 입력하거나 둘 다 비워 주세요")
        public boolean isPaired() {
            return (refLlmtNvl == null) == (refUplmtNvl == null);
        }

        @JsonIgnore
        @AssertTrue(message = "참고범위 하한은 상한보다 클 수 없습니다")
        public boolean isOrdered() {
            return refLlmtNvl == null || refUplmtNvl == null || refLlmtNvl.compareTo(refUplmtNvl) <= 0;
        }
    }
}
