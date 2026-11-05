package com.nyangmeong.ai.dto;

import java.time.LocalDateTime;

import com.nyangmeong.ai.domain.AiRoutMpng;

/**
 * [관리자 라우팅 매핑표 한 줄]
 * - 사용 안 함 행 포함 (FN-ADM-080-01)
 */
public record AdminAiRouteSummary(Long aiRoutMpngSn, String domnNm, String trgtScrnNm, String scrnId,
        String scrnParamCn, String instKwrdCn, String mvmnMthdCd, boolean rprsChipYn, Integer prrtSeq,
        String rmrkCn, boolean useYn, LocalDateTime mdfcnDt) {

    public static AdminAiRouteSummary from(AiRoutMpng route) {
        return new AdminAiRouteSummary(route.getAiRoutMpngSn(), route.getDomnNm(), route.getTrgtScrnNm(),
                route.getScrnId(), route.getScrnParamCn(), route.getInstKwrdCn(), route.getMvmnMthdCd(),
                "Y".equals(route.getRprsChipYn()), route.getPrrtSeq(), route.getRmrkCn(),
                "Y".equals(route.getUseYn()), route.getMdfcnDt());
    }
}
