package com.nyangmeong.ai.dto;

import jakarta.validation.constraints.Size;

/**
 * [관리자 키워드 등록·수정 요청]
 * - 등록 : dictUsgCd·kwrdClsfNm·artclNm·kwrdCn 필수, 결과가 코드인 분류는 mpngCd 필수 (FN-ADM-081-02)
 * - 수정 : 보낸 칸만 바뀜. 해석 결과·연결 코드·비고는 빈 문자열을 보내면 비운다. 용도·분류는 바꾸지 않는다
 */
public record AdminAiKeywordSaveRequest(
        String dictUsgCd,
        @Size(max = 50) String kwrdClsfNm,
        @Size(max = 100) String artclNm,
        @Size(max = 1000) String kwrdCn,
        @Size(max = 500) String intrpRsltCn,
        String mpngCd,
        @Size(max = 1000) String rmrkCn,
        Boolean useYn) {
}
