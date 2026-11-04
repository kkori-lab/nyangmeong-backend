package com.nyangmeong.pet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * [관리자 품종 수정 요청]
 * - 보낸 칸만 바뀜, 종·코드는 안 바꿈 (FN-ADM-101-01)
 */
public record AdminBreedUpdateRequest(
        String vrtyNm,
        @Min(0) @Max(999_999_999) Integer sortSeq,
        Boolean useYn) {
}
