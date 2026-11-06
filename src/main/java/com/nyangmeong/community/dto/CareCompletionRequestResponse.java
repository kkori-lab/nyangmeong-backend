package com.nyangmeong.community.dto;

import java.time.LocalDateTime;

import com.nyangmeong.community.domain.CareCompletionRequest;

/**
 * [돌봄 완료 요청 회차 응답]
 * - 입력 항목이 없는 요청이라 회차·종료 시각·보호자 확인 결과만 준다
 */
public record CareCompletionRequestResponse(
        Long prtcCmptnDmndCyclSn,
        Long petPrtcMtchSn,
        Long mtchCndVerSn,
        Integer dmndCycl,
        LocalDateTime prtcEndDt,
        String prtcrIdntyRsltCd,
        LocalDateTime prtcrIdntyDt) {

    public static CareCompletionRequestResponse from(CareCompletionRequest cycle) {
        return new CareCompletionRequestResponse(cycle.getPrtcCmptnDmndCyclSn(), cycle.getPetPrtcMtchSn(),
                cycle.getMtchCndVerSn(), cycle.getDmndCycl(), cycle.getPrtcEndDt(), cycle.getPrtcrIdntyRsltCd(),
                cycle.getPrtcrIdntyDt());
    }
}
