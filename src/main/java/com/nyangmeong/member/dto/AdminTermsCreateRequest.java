package com.nyangmeong.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [관리자 약관 새 버전 등록 요청]
 * - 종류(trmsFixIdntfKey)는 이미 있는 것만, 새 종류는 만들지 않는다
 * - 글자 수는 앞뒤 공백 지운 뒤 기준 (서비스에서 다시 확인)
 */
public record AdminTermsCreateRequest(
        @NotBlank String trmsFixIdntfKey,
        @NotBlank @Size(max = 80) String trmsVerId,
        @NotBlank @Size(max = 200) String trmsTtl,
        @NotBlank @Size(max = 4000) String trmsCn,
        @NotNull Boolean esntlAgreYn) {
}
