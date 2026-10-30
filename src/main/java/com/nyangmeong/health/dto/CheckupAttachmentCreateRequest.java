package com.nyangmeong.health.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * [검진기록 첨부 등록 요청]
 * - multipart/form-data : 영상 사진이면 영상 종류·촬영 부위 필수 (서비스에서 검증)
 */
public record CheckupAttachmentCreateRequest(
        @NotNull Long ckupRcdSn,
        @NotNull MultipartFile file,
        @NotBlank String atchTypeCd,
        String imgKndCd,
        String shtRegnCd) {
}
