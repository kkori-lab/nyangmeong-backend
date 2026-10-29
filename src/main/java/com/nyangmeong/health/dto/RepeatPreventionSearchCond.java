package com.nyangmeong.health.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * [반복 예방 목록 검색 조건]
 * - 검진기록 1건 단위 (FN-HLT-005-05)
 */
@Data
public class RepeatPreventionSearchCond {

    /** 검진기록일련번호 */
    @NotNull
    private Long ckupRcdSn;
}
