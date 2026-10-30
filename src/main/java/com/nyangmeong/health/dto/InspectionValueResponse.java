package com.nyangmeong.health.dto;

import java.math.BigDecimal;

import com.nyangmeong.health.domain.InspectionValue;

/**
 * [검사 수치 응답]
 * - 직접입력이 아니면 항목명은 null
 */
public record InspectionValueResponse(
        Long inspNvlSn,
        Long ckupRcdSn,
        String inspArtclCd,
        String inspArtclDrctInptNm,
        BigDecimal rsltNvl,
        String unitNm,
        BigDecimal refLlmtNvl,
        BigDecimal refUplmtNvl,
        String memoCn,
        Integer sortSeq) {

    public static InspectionValueResponse from(InspectionValue value) {
        String drctInptNm = value.getInspArtclDrctInptNm() == null || value.getInspArtclDrctInptNm().isEmpty()
                ? null
                : value.getInspArtclDrctInptNm();
        return new InspectionValueResponse(value.getInspNvlSn(), value.getCkupRcdSn(), value.getInspArtclCd(),
                drctInptNm, value.getRsltNvl(), value.getUnitNm(), value.getRefLlmtNvl(), value.getRefUplmtNvl(),
                value.getMemoCn(), value.getSortSeq());
    }
}
