package com.nyangmeong.health.dto;

import jakarta.validation.constraints.Size;

/**
 * [검진기록 수정 요청]
 * - 보낸 값만 바꾼다. 검진일·반려동물은 연결 일정 수정에서만 바꾼다 (※신규 정책)
 */
public record CheckupUpdateRequest(
        @Size(max = 80) String hsptlNm,
        String ckupTypeCd,
        @Size(max = 500) String ckupCn,
        @Size(max = 200) String ckupMemoCn) {
}
