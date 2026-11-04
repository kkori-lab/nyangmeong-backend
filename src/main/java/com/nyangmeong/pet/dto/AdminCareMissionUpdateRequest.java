package com.nyangmeong.pet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * [관리자 케어미션 수정 요청]
 * - 보낸 칸만 바뀜, 미션명·풀·이동 경로는 안 바꿈 (FN-ADM-071-02)
 */
public record AdminCareMissionUpdateRequest(
        @Min(1) @Max(1000) Integer cmpnPnt,
        @Min(1) @Max(10) Integer achvCrtrNmtm,
        Boolean useYn) {
}
