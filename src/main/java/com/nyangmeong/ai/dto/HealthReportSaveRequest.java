package com.nyangmeong.ai.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * [AI 건강분석 리포트 저장 요청]
 */
public record HealthReportSaveRequest(
        @NotNull LocalDate baseYmd,
        @NotBlank @Size(max = 20) String rptTtl,
        @NotNull @Size(min = 1, max = 3) List<@NotBlank @Size(max = 40) String> tips,
        @NotNull @Pattern(regexp = "^[0-9a-fA-F]{64}$") String inputHash,
        @NotNull Boolean rfrshYn) {
}
