package com.nyangmeong.health.dto;

import com.nyangmeong.health.domain.EtcInspection;

/**
 * [기타검사 응답]
 */
public record EtcInspectionResponse(
        Long etcInspSn,
        Long ckupRcdSn,
        String inspNm,
        String inspRsltCn,
        String memoCn) {

    public static EtcInspectionResponse from(EtcInspection insp) {
        return new EtcInspectionResponse(insp.getEtcInspSn(), insp.getCkupRcdSn(), insp.getInspNm(),
                insp.getInspRsltCn(), insp.getMemoCn());
    }
}
