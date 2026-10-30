package com.nyangmeong.health.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [관찰 회차 추가 요청]
 * - multipart/form-data : 종료된 항목은 추가 불가 (FN-HLT-002-20~25·32)
 */
public record ObservationCycleCreateRequest(
        @NotNull Long obsrvArtclSn,
        @NotNull MultipartFile file,
        @NotNull LocalDateTime obsrvDt,
        @Digits(integer = 5, fraction = 3) BigDecimal obsrvSz,
        @Size(max = 8) String obsrvChgCd,
        @Size(max = 200) String memoCn) {
}
