package com.nyangmeong.health.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [관찰 항목 등록 요청]
 * - multipart/form-data : 항목과 1회차를 함께 저장, 1회차는 변화 입력 없음 (FN-HLT-002-20~25·32)
 */
public record ObservationItemCreateRequest(
        @NotBlank @Size(max = 40) String obsrvArtclNm,
        @NotBlank @Size(max = 8) String obsrvRegnCd,
        @Size(max = 40) String obsrvRegnDtlNm,
        @NotNull MultipartFile file,
        @NotNull LocalDateTime obsrvDt,
        @Digits(integer = 5, fraction = 3) BigDecimal obsrvSz,
        @Size(max = 200) String memoCn) {
}
