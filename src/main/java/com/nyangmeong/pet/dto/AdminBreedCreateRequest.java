package com.nyangmeong.pet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * [관리자 품종 추가 요청]
 * - 이름은 앞뒤 공백 지운 뒤 1~50자 (서비스에서 확인)
 * - sortSeq 없으면 그 종 마지막 + 10, useYn 없으면 사용
 */
public record AdminBreedCreateRequest(
        @NotBlank String petSeCd,
        @NotNull String vrtyNm,
        @Min(0) @Max(999_999_999) Integer sortSeq,
        Boolean useYn) {
}
