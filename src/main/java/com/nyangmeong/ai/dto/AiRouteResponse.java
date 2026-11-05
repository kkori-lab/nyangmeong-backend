package com.nyangmeong.ai.dto;

import java.util.Arrays;
import java.util.List;

import com.nyangmeong.ai.domain.AiRoutMpng;

/**
 * [AI 서버용 라우팅 매핑 한 줄]
 * - 사용 중 행만 (FN-ADM-080-04). 지시어는 쉼표로 나눠 앞뒤 공백을 뺀 배열
 */
public record AiRouteResponse(Long aiRoutMpngSn, String trgtScrnNm, String scrnId, String scrnParamCn,
        List<String> instKwrds, String mvmnMthdCd, boolean rprsChipYn, Integer prrtSeq) {

    public static AiRouteResponse from(AiRoutMpng route) {
        return new AiRouteResponse(route.getAiRoutMpngSn(), route.getTrgtScrnNm(), route.getScrnId(),
                route.getScrnParamCn(), splitKeywords(route.getInstKwrdCn()), route.getMvmnMthdCd(),
                "Y".equals(route.getRprsChipYn()), route.getPrrtSeq());
    }

    private static List<String> splitKeywords(String instKwrdCn) {
        if (instKwrdCn == null) {
            return List.of();
        }
        return Arrays.stream(instKwrdCn.split(",")).map(String::strip).filter(k -> !k.isEmpty()).toList();
    }
}
