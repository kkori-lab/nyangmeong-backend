package com.nyangmeong.pet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [관리자 포인트 조정 요청]
 * - SCR-ADM-072 (FN-ADM-072-02), dlngPnt는 지급 +·차감 -, 0은 서비스에서 막음
 */
public record AdminPointAdjustRequest(
        /** 회원일련번호 */
        @NotNull Long mbrSn,
        /** 거래포인트 */
        @NotNull @Min(-AdminPointAdjustRequest.MAX_PNT) @Max(AdminPointAdjustRequest.MAX_PNT) Integer dlngPnt,
        /** 사유 (원장 메모) */
        @NotBlank @Size(max = 200) String memoCn) {

    /** 1회 조정 상한 */
    public static final int MAX_PNT = 100_000;
}
