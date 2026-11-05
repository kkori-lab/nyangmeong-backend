package com.nyangmeong.ai.dto;

import com.nyangmeong.ai.domain.KwrdDict;

/**
 * [AI 서버용 키워드 사전 한 줄]
 * - 사용 중 행만. kwrdCn은 관리자가 쓴 예시 문구 그대로(정규식 아님)
 */
public record AiKeywordResponse(Long kwrdDictSn, String kwrdClsfNm, String artclNm, String kwrdCn,
        String intrpRsltCn, String mpngCd) {

    public static AiKeywordResponse from(KwrdDict keyword) {
        return new AiKeywordResponse(keyword.getKwrdDictSn(), keyword.getKwrdClsfNm(), keyword.getArtclNm(),
                keyword.getKwrdCn(), keyword.getIntrpRsltCn(), keyword.getMpngCd());
    }
}
