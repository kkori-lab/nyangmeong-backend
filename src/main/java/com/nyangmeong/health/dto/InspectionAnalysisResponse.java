package com.nyangmeong.health.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * [검사결과지 분석 응답]
 * - 저장하지 않는 인식 결과 : 보호자가 SCR-HLT-007에서 확인 후 저장 (FN-HLT-005-04)
 * - anlysRslt : AnalysisResult 이름(SUCCESS·NOT_RECOGNIZED·TEMPORARY_ERROR)
 */
public record InspectionAnalysisResponse(
        Long ckupRcdAtchSn,
        Long ckupRcdSn,
        String anlysRslt,
        List<InspectionAnalysisItem> items,
        List<String> excludedArtclNms) {

    /**
     * [인식한 혈액검사 항목]
     * - registered : 같은 검진기록에 이미 있는 항목
     */
    public record InspectionAnalysisItem(
            String inspArtclCd,
            String inspArtclDrctInptNm,
            BigDecimal rsltNvl,
            String unitNm,
            BigDecimal refLlmtNvl,
            BigDecimal refUplmtNvl,
            Boolean registered) {
    }
}
