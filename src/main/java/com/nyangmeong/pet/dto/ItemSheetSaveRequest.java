package com.nyangmeong.pet.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;

/**
 * [착용 시트 저장 요청]
 * - Python AI 서버 -> Java, multipart/form-data (FN-MYP-011-01)
 */
public record ItemSheetSaveRequest(@NotNull MultipartFile sheet) {
}
