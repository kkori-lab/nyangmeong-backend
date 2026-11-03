package com.nyangmeong.community.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * [실종 제보 등록 요청]
 * - multipart/form-data, 이미지 최대 10장 (FN-CMU-002-10·13)
 * - 실종지역은 받지 않고 목격 장소 좌표로 서버가 계산한다
 * - vrtySn·vrtyDrctInptNm 중 하나만 보낸다
 */
public record LostReportCreateRequest(
        @NotBlank String petSeCd,
        @NotBlank @Size(max = 20) @Pattern(regexp = "(?s).*\\S.*") String petNm,
        Long vrtySn,
        @Size(max = 20) @Pattern(regexp = "(?s).*\\S.*") String vrtyDrctInptNm,
        @NotBlank @Size(max = 20) @Pattern(regexp = "(?s).*\\S.*") String colrNm,
        @Size(max = 40) String chrcCn,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastWtnsDt,
        @Size(max = 200) String wtnsPlcNm,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal wtnsPlcLat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal wtnsPlcLot,
        @NotBlank @Pattern(regexp = "[0-9-]{9,13}") String cnpl,
        @NotBlank @Size(max = 50) @Pattern(regexp = "(?s).*\\S.*") String pstTtl,
        @NotBlank @Size(max = 2000) @Pattern(regexp = "(?s).*\\S.*") String pstCn,
        List<MultipartFile> images,
        @NotNull Boolean prvcRlsAgreYn) implements LostReportInput {
}
