package com.nyangmeong.ai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * [관리자 라우팅 매핑 등록·수정 요청]
 * - 등록 : trgtScrnNm·mvmnMthdCd 필수, prrtSeq가 없으면 맨 아래 (FN-ADM-080-02·03)
 * - 수정 : 보낸 칸만 바뀜. 화면 ID·파라미터·지시어·비고는 빈 문자열을 보내면 비운다. prrtSeq가 없으면 순위 그대로
 * - 도메인명은 받지 않고 서버가 화면 ID로 정한다
 */
public record AdminAiRouteSaveRequest(
        @Size(max = 100) String trgtScrnNm,
        @Size(max = 100) String scrnId,
        @Size(max = 200) String scrnParamCn,
        @Size(max = 1000) String instKwrdCn,
        String mvmnMthdCd,
        Boolean rprsChipYn,
        @Min(1) Integer prrtSeq,
        @Size(max = 1000) String rmrkCn,
        Boolean useYn) {
}
