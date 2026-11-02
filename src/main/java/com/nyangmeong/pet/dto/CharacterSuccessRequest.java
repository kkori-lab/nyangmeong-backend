package com.nyangmeong.pet.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;

/**
 * [캐릭터 생성 성공 보고]
 * - Python AI 서버 -> Java, multipart/form-data (FN-MYP-010-09·17)
 * - image 512 PNG, sheet 맨몸 2×2 시트 1024 PNG, 나머지는 시트 배치값
 */
public record CharacterSuccessRequest(@NotNull MultipartFile image, @NotNull MultipartFile sheet,
        @NotNull Double scale, @NotNull Double originX, @NotNull Double originY) {
}
