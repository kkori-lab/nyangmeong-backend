package com.nyangmeong.ai.dto;

import com.nyangmeong.ai.domain.KwrdDict;

/**
 * [관리자 키워드 사전 한 줄]
 * - 사용 안 함 행 포함 (FN-ADM-081-01). mpngCdNm은 연결 코드 이름(화면 표시용)
 */
public record AdminAiKeywordSummary(Long kwrdDictSn, String dictUsgCd, String kwrdClsfNm, String artclNm,
        String kwrdCn, String intrpRsltCn, String mpngCd, String mpngCdNm, Integer sortSeq, String rmrkCn,
        boolean useYn) {

    public static AdminAiKeywordSummary from(KwrdDict keyword) {
        return new AdminAiKeywordSummary(keyword.getKwrdDictSn(), keyword.getDictUsgCd(), keyword.getKwrdClsfNm(),
                keyword.getArtclNm(), keyword.getKwrdCn(), keyword.getIntrpRsltCn(), keyword.getMpngCd(),
                keyword.getMpngCdNm(), keyword.getSortSeq(), keyword.getRmrkCn(), "Y".equals(keyword.getUseYn()));
    }
}
