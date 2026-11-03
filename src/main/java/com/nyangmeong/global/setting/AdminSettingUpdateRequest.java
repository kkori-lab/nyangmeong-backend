package com.nyangmeong.global.setting;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * [운영 설정 수정 요청]
 * - PATCH /api/admin/settings : 보낸 필드만 바꿈, 모두 선택
 * - 범위 : DDL 자릿수 안의 제안값 (docs/api/global.md)
 */
public record AdminSettingUpdateRequest(
        @Min(1) @Max(100) Long petMaxCnt,
        @Min(1) @Max(100) Long jntBrupMaxNope,
        @Min(1) @Max(3650) Long invtnVldDayCnt,
        @Min(1) @Max(100) Long lgnFailLmtNmtm,
        @Min(1) @Max(20) Long dshbdCareMaxExpsrCnt,
        @Min(1) @Max(100) Long pstImgMaxCnt,
        @Min(1) @Max(100) Long blindDclrAcmlCnt,
        @Min(1) @Max(99) Long lostTpofExpryCrtrDayCnt,
        @Min(1) @Max(99) Long mtchNoRspnsEndCrtrDayCnt,
        @Pattern(regexp = "^(20[2-9][0-9]|2100)$") String lowstHrwgAplcnYr,
        @Min(0) @Max(1_000_000) Long lowstHrwgAmt,
        @Size(min = 1, max = 1000) @Pattern(regexp = "(?s).*\\S.*") String dscvAutoCmntCn,
        @Size(min = 1, max = 1000) @Pattern(regexp = "(?s).*\\S.*") String aiCmtryBscCn,
        @Min(1) @Max(99) Long notiHideCrtrDayCnt,
        @Min(1) @Max(50) Long aiCnvrsTurnCnt,
        @Min(500) @Max(100_000) Long aiTknUplmtCnt,
        @Min(1_048_576) @Max(52_428_800) Long fileMaxSz) {
}
