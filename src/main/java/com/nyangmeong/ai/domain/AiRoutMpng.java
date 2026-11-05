package com.nyangmeong.ai.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [AI 라우팅 매핑 VO]
 * - AI_ROUT_MPNG : 대화창 지시어 → 이동할 화면 (FN-ADM-080, FN-AIM-004-09)
 */
@Data
public class AiRoutMpng {

    /** AI라우팅매핑일련번호 */
    private Long aiRoutMpngSn;
    /** 도메인명 */
    private String domnNm;
    /** 대상화면명 */
    private String trgtScrnNm;
    /** 화면ID */
    private String scrnId;
    /** 화면파라미터내용 */
    private String scrnParamCn;
    /** 지시어키워드내용 (쉼표 구분) */
    private String instKwrdCn;
    /** 이동방식공통코드 (AIMG09) */
    private String mvmnMthdCd;
    /** 대표칩여부 */
    private String rprsChipYn;
    /** 우선순위 */
    private Integer prrtSeq;
    /** 비고내용 */
    private String rmrkCn;
    /** 사용여부 */
    private String useYn;
    /** 수정일시 */
    private LocalDateTime mdfcnDt;
}
