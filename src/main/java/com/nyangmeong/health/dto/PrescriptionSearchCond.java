package com.nyangmeong.health.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * [처방약 목록 검색 조건]
 * - 검진기록 1건 단위 (FN-HLT-005-03)
 */
@Data
public class PrescriptionSearchCond {

    /** 검진기록일련번호 */
    @NotNull
    private Long ckupRcdSn;
    /** 처방상태공통코드(HLTG06) */
    private String prscSttsCd;
}
