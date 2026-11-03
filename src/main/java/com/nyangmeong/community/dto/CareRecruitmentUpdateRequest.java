package com.nyangmeong.community.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * [펫돌봄 모집 수정 요청]
 * - multipart/form-data, 보낸 필드만 바뀐다(null 은 그대로)
 * - pets·prtcShpCds 를 보내면 목록 교체, prtcPlcLat·prtcPlcLot 은 함께 보내며 보내면 돌봄 지역을 다시 계산한다
 * - keepAtchFileSns 를 보내면 그 목록에 없는 기존 이미지는 삭제, 보내지 않으면 기존 이미지 유지
 */
public record CareRecruitmentUpdateRequest(
        @Size(min = 1, max = 2) List<@Valid CareRecruitmentPetRequestItem> pets,
        @Size(min = 1) List<String> prtcShpCds,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime prtcBgngDt,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime prtcEndDt,
        Boolean schdlCnsltnPsbltyYn,
        String cstUnitCd,
        @Positive Integer cstAmt,
        @Size(min = 1, max = 100) @Pattern(regexp = "(?s).*\\S.*") String rlsRgnNm,
        @Size(min = 1, max = 200) @Pattern(regexp = "(?s).*\\S.*") String prtcPlcDaddr,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal prtcPlcLat,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal prtcPlcLot,
        @Size(min = 1, max = 50) @Pattern(regexp = "(?s).*\\S.*") String pstTtl,
        @Size(min = 1, max = 2000) @Pattern(regexp = "(?s).*\\S.*") String pstCn,
        List<Long> keepAtchFileSns,
        List<MultipartFile> images) {
}
