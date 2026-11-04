package com.nyangmeong.expense.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * [영수증 원본 저장 요청]
 * - Python AI 서버 -> Java, multipart/form-data (FN-AIM-008-09·013-08, FN-MYP-031-03)
 * - 요청 출처 필수 : 건강 일지 AIMC0026, 가계부 AIMC0027, AI 대화창 AIMC0031 (FN-AIM-013-04)
 * - petSn : 가계부 출처만 생략 가능(반려동물은 확인 모달에서 고름), 그 밖은 필수 (ReceiptService)
 */
public record ReceiptOriginalRequest(@NotNull MultipartFile file, Long petSn,
        @NotNull @Pattern(regexp = "AIMC0026|AIMC0027|AIMC0031") String ocrDmndSrcCd) {
}
