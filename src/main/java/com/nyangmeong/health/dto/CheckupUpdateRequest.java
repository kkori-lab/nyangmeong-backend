package com.nyangmeong.health.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

/**
 * [검진기록 수정 요청]
 * - 보낸 값만 바꾼다. petSn·ckupYmd 가 바뀌면 일정을 다시 연결
 */
public record CheckupUpdateRequest(
        Long petSn,
        LocalDate ckupYmd,
        @Size(max = 80) String hsptlNm,
        String ckupTypeCd,
        @Size(max = 500) String ckupCn,
        @Size(max = 200) String ckupMemoCn) {
}
