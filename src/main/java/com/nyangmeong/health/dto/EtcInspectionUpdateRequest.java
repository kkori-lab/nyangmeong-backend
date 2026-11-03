package com.nyangmeong.health.dto;

import jakarta.validation.constraints.Size;

/**
 * [기타검사 수정 요청]
 * - 보낸 항목만 수정 : 검사명·결과는 빈 값 불가 (서비스에서 검증)
 */
public record EtcInspectionUpdateRequest(
        @Size(max = 100) String inspNm,
        @Size(max = 500) String inspRsltCn,
        @Size(max = 200) String memoCn) {
}
