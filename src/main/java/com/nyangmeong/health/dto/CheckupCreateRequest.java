package com.nyangmeong.health.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [검진기록 직접 등록 요청]
 * - schdlSn: [옮겨 연결하기]를 고른 경우만 (FN-HLT-002-46)
 */
public record CheckupCreateRequest(
        @NotNull LocalDate ckupYmd,
        @NotBlank @Size(max = 80) String hsptlNm,
        @NotBlank String ckupTypeCd,
        @Size(max = 500) String ckupCn,
        @Size(max = 200) String ckupMemoCn,
        Long schdlSn) {
}
