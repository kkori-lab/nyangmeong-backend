package com.nyangmeong.community.dto;

import java.time.LocalDateTime;

import com.nyangmeong.community.domain.CareMatchCondition;

/**
 * [매칭 조건 항목]
 */
public record CareMatchConditionItem(
        Long mtchCndVerSn,
        Integer cndVerNo,
        LocalDateTime acrdPrtcBgngDt,
        LocalDateTime acrdPrtcEndDt,
        boolean schdlCnsltnPsbltyYn,
        String acrdPrtcCn,
        String cstUnitCd,
        Integer acrdCstAmt) {

    public static CareMatchConditionItem from(CareMatchCondition cnd) {
        return new CareMatchConditionItem(cnd.getMtchCndVerSn(), cnd.getCndVerNo(), cnd.getAcrdPrtcBgngDt(),
                cnd.getAcrdPrtcEndDt(), "Y".equals(cnd.getSchdlCnsltnPsbltyYn()), cnd.getAcrdPrtcCn(),
                cnd.getCstUnitCd(), cnd.getAcrdCstAmt());
    }
}
