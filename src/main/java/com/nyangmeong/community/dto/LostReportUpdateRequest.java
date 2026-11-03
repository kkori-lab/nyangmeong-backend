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
 * [실종 제보 수정 요청]
 * - multipart/form-data, 등록과 같은 항목을 모두 다시 보낸다(수정 범위 전체, 화면설계서 SCR-CMU-002-06)
 * - keepAtchFileSns 를 보내면 그 목록에 없는 기존 이미지는 삭제, 보내지 않으면 기존 이미지 유지
 * - 목격 장소 좌표가 그대로면 실종지역을 유지하고 바뀌면 다시 계산한다
 */
public record LostReportUpdateRequest(
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
        List<Long> keepAtchFileSns,
        List<MultipartFile> images,
        @NotNull Boolean prvcRlsAgreYn) implements LostReportInput {
}
