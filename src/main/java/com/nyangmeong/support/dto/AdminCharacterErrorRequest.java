package com.nyangmeong.support.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * [관리자 캐릭터 오류 처리 요청]
 * - POST /api/admin/inquiries/{otoInqrySn}/character-error (FN-ADM-042-07)
 * - charErrPrcsRsltCd : CHRG04 (CHRC0008 무료 재생성, CHRC0009 원래 캐릭터 복원)
 */
public record AdminCharacterErrorRequest(
        @NotBlank(message = "처리 결과를 선택해 주세요.") String charErrPrcsRsltCd) {
}
