package com.nyangmeong.expense.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * [영수증 업로드 요청]
 * - multipart/form-data, 이미지 1건 + 대상 반려동물 (FN-AIM-008-03·04)
 * - 요청 출처 : 건강 일지 AIMC0026(생략 시), AI 대화창 AIMC0031 (FN-AIM-013-04)
 */
public record ReceiptCreateRequest(@NotNull MultipartFile file, @NotNull Long petSn,
        @Pattern(regexp = "AIMC0026|AIMC0031") String ocrDmndSrcCd) {
}
