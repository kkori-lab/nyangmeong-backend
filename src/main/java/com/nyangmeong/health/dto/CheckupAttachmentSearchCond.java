package com.nyangmeong.health.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * [검진기록 첨부 목록 검색 조건]
 * - 검진기록 1건 단위 (FN-HLT-005-01)
 */
@Data
public class CheckupAttachmentSearchCond {

    /** 검진기록일련번호 */
    @NotNull
    private Long ckupRcdSn;
    /** 첨부유형공통코드(HLTG20) */
    private String atchTypeCd;
}
