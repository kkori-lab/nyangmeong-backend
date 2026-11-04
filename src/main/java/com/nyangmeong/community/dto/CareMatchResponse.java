package com.nyangmeong.community.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.nyangmeong.community.domain.CareCompletionRequest;
import com.nyangmeong.community.domain.CareMatch;
import com.nyangmeong.community.domain.CareMatchCondition;

/**
 * [펫돌봄 매칭 응답]
 * - cnd : 현재(가장 최근) 조건 버전
 * - cmptnDmnds : 완료 요청 회차 목록(회차 순)
 */
public record CareMatchResponse(
        Long petPrtcMtchSn,
        Long petPrtcRcrtSn,
        Long otoChatRoomSn,
        String mtchDlngSttsCd,
        boolean rcvYn,
        LocalDateTime rcvIdntyDt,
        CareMatchConditionItem cnd,
        List<CareCompletionRequestResponse> cmptnDmnds) {

    public static CareMatchResponse of(CareMatch match, CareMatchCondition cnd,
            List<CareCompletionRequest> cycles) {
        return new CareMatchResponse(match.getPetPrtcMtchSn(), match.getPetPrtcRcrtSn(), match.getOtoChatRoomSn(),
                match.getMtchDlngSttsCd(), "Y".equals(match.getRcvYn()), match.getRcvIdntyDt(),
                CareMatchConditionItem.from(cnd),
                cycles.stream().map(CareCompletionRequestResponse::from).toList());
    }
}
