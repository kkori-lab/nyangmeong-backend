package com.nyangmeong.health.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

/**
 * [관찰 회차 수정 요청]
 * - multipart/form-data : 보낸 항목만 수정, 사진을 보내면 교체 (FN-HLT-002-19)
 */
public record ObservationCycleUpdateRequest(
        MultipartFile file,
        LocalDateTime obsrvDt,
        @Digits(integer = 5, fraction = 3) BigDecimal obsrvSz,
        @Size(max = 8) String obsrvChgCd,
        @Size(max = 200) String memoCn) {
}
