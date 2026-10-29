package com.nyangmeong.health.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [검사 수치 저장 요청]
 * - 혈액검사 고정 : 같은 검진기록에 이미 있는 항목은 제외, 삭제된 같은 항목은 복구 (FN-HLT-007-02·03)
 */
public record InspectionValueCreateRequest(
        @NotNull Long ckupRcdSn,
        @NotEmpty List<@NotNull @Valid InspectionValueItemRequest> values) {

    /**
     * [검사 수치 1행]
     * - 항목 코드(HLTG12)와 직접입력명 중 하나는 필수 (서비스에서 검증)
     */
    public record InspectionValueItemRequest(
            @Size(max = 8) String inspArtclCd,
            @Size(max = 40) String inspArtclDrctInptNm,
            @NotNull @Digits(integer = 11, fraction = 4) BigDecimal rsltNvl,
            @NotBlank @Size(max = 20) String unitNm,
            @Digits(integer = 11, fraction = 4) BigDecimal refLlmtNvl,
            @Digits(integer = 11, fraction = 4) BigDecimal refUplmtNvl,
            @Size(max = 200) String memoCn) {
    }
}
