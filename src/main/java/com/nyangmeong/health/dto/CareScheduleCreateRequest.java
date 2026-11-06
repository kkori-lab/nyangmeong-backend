package com.nyangmeong.health.dto;

import java.time.LocalDateTime;

import com.nyangmeong.global.aitool.AiCode;
import com.nyangmeong.global.aitool.AiRequiredIf;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * [일정 등록 요청]
 * - 종류별 필수값(병원: 검진유형·병원명, 그 외: 일정명)은 서비스에서 재검증
 * - @AiRequiredIf : AI 매니저 도구 목록에 같은 규칙 표시
 */
@AiRequiredIf(field = "ckupTypeCd", when = "schdlKndCd", equals = "HLTC0057")
@AiRequiredIf(field = "hsptlNm", when = "schdlKndCd", equals = "HLTC0057")
@AiRequiredIf(field = "schdlNm", when = "schdlKndCd", equals = "HLTC0058")
@AiRequiredIf(field = "schdlNm", when = "schdlKndCd", equals = "HLTC0059")
@AiRequiredIf(field = "schdlNm", when = "schdlKndCd", equals = "HLTC0060")
public record CareScheduleCreateRequest(
        @NotBlank @AiCode("HLTG19") String schdlKndCd,
        @NotNull LocalDateTime schdlDt,
        @NotNull Boolean hrInptYn,
        @AiCode("HLTG01") String ckupTypeCd,
        @Size(max = 80) String hsptlNm,
        @Size(max = 40) String schdlNm,
        @Size(max = 500) String memoCn,
        @AiCode("HLTG21") String cldrBadgeColrCd,
        @AiCode("HLTG22") String schdlSrcCd) {
}
