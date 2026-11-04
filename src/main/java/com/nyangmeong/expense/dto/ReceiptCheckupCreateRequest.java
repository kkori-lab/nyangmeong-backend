package com.nyangmeong.expense.dto;

import java.time.LocalDate;

import com.nyangmeong.health.dto.CheckupCreateRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [영수증 [직접 입력] 검진기록 등록 요청]
 * - 영수증 인식 실패·일시 오류 뒤 [직접 입력]으로 연 검진기록 등록 모달 (FN-HLT-002-31)
 * - 검진기록 값은 직접 등록(CheckupCreateRequest)과 같고, 중복 클릭·재전송을 막는 멱등 요청 ID 를 더한다
 */
public record ReceiptCheckupCreateRequest(
        @NotBlank @Size(max = 100) String idmptDmndId,
        @NotNull LocalDate ckupYmd,
        @NotBlank @Size(max = 80) String hsptlNm,
        @NotBlank String ckupTypeCd,
        @Size(max = 500) String ckupCn,
        @Size(max = 200) String ckupMemoCn,
        Long schdlSn) {

    /**
     * 검진기록 직접 등록 값
     */
    public CheckupCreateRequest toCheckupCreateRequest() {
        return new CheckupCreateRequest(ckupYmd, hsptlNm, ckupTypeCd, ckupCn, ckupMemoCn, schdlSn);
    }
}
