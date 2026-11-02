package com.nyangmeong.global.log;

import java.time.LocalDateTime;

import tools.jackson.databind.JsonNode;

/**
 * [관리자 작업 로그 상세 응답]
 */
public record AdminTaskLogDetailResponse(
        Long mngrTaskLogSn,
        LocalDateTime regDt,
        Long mbrSn,
        String mbrNcnm,
        String taskTypeCd,
        String trgtTblNm,
        Long trgtSn,
        String taskSmryCn,
        JsonNode chgBfrCn,
        JsonNode chgAftrCn,
        String cntnIpAddr) {

    public static AdminTaskLogDetailResponse from(AdminTaskLog log, String mbrNcnm, JsonNode chgBfrCn,
            JsonNode chgAftrCn) {
        return new AdminTaskLogDetailResponse(log.getMngrTaskLogSn(), log.getRegDt(), log.getMbrSn(), mbrNcnm,
                log.getTaskTypeCd(), log.getTrgtTblNm(), log.getTrgtSn(), log.getTaskSmryCn(), chgBfrCn, chgAftrCn,
                log.getCntnIpAddr());
    }
}
