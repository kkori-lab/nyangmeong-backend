package com.nyangmeong.community.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * [관리자 건강팁 지정 요청]
 * - FN-ADM-023-02 : 노출 자리 1~3
 */
public record AdminHealthTipAssignRequest(@NotNull @Min(1) @Max(3) Integer hlthInfoExpsrSeq) {
}
