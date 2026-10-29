package com.nyangmeong.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [기타검사 입력 요청]
 * - 결과는 텍스트만 (FN-HLT-005-11~13)
 */
public record EtcInspectionCreateRequest(
        @NotNull Long ckupRcdSn,
        @NotBlank @Size(max = 100) String inspNm,
        @NotBlank @Size(max = 500) String inspRsltCn,
        @Size(max = 200) String memoCn) {
}
