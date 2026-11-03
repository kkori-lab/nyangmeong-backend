package com.nyangmeong.community.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * [펫돌봄 모집 등록 요청]
 * - multipart/form-data, 이미지 최대 10장 (FN-CMU-004-06~11·13)
 * - 돌봄 지역은 받지 않고 장소 좌표로 서버가 계산한다
 * - 일정 협의 가능이 아니면 prtcBgngDt·prtcEndDt 필수
 */
public record CareRecruitmentCreateRequest(
        @NotEmpty @Size(max = 2) List<@Valid CareRecruitmentPetRequestItem> pets,
        @NotEmpty List<String> prtcShpCds,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime prtcBgngDt,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime prtcEndDt,
        @NotNull Boolean schdlCnsltnPsbltyYn,
        @NotBlank String cstUnitCd,
        @NotNull @Positive Integer cstAmt,
        @NotBlank @Size(max = 100) String rlsRgnNm,
        @NotBlank @Size(max = 200) String prtcPlcDaddr,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal prtcPlcLat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal prtcPlcLot,
        @NotBlank @Size(max = 50) @Pattern(regexp = "(?s).*\\S.*") String pstTtl,
        @NotBlank @Size(max = 2000) @Pattern(regexp = "(?s).*\\S.*") String pstCn,
        List<MultipartFile> images,
        @NotNull Boolean utztnRulAgreYn) {
}
