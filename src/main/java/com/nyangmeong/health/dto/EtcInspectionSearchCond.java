package com.nyangmeong.health.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * [기타검사 목록 검색 조건]
 * - 검진기록 1건 단위 (FN-HLT-005-02)
 */
@Data
public class EtcInspectionSearchCond {

    /** 검진기록일련번호 */
    @NotNull
    private Long ckupRcdSn;
}
