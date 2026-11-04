package com.nyangmeong.pet.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;

/**
 * [착용 모습 대표 이미지 저장 요청]
 * - Python AI 서버 -> Java, multipart/form-data (FN-MYP-010-16)
 * - itemSns : 합성에 쓴 아이템 번호, 맨몸이면 없음
 */
public record EquippedImageSaveRequest(@NotNull MultipartFile image, List<Long> itemSns) {
}
