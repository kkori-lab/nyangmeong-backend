package com.nyangmeong.expense.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * [영수증 원본 저장 요청]
 * - Python AI 서버 -> Java, multipart/form-data (FN-AIM-008-09·013-08)
 * - 요청 출처 필수 : 건강 일지 AIMC0026, AI 대화창 AIMC0031 (FN-AIM-013-04)
 */
public record ReceiptOriginalRequest(@NotNull MultipartFile file, @NotNull Long petSn,
        @NotNull @Pattern(regexp = "AIMC0026|AIMC0031") String ocrDmndSrcCd) {
}
