package com.nyangmeong.health.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * [검사 수치 목록 검색 조건]
 * - 검진기록 1건 단위 (FN-HLT-005-02)
 */
@Data
public class InspectionValueSearchCond {

    /** 검진기록일련번호 */
    @NotNull
    private Long ckupRcdSn;
    /** 검사항목공통코드(HLTG12) */
    private String inspArtclCd;
}
