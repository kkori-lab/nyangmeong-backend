package com.nyangmeong.health.dto;

import lombok.Data;

/**
 * [검사 수치 목록 검색 조건]
 * - 검진기록 1건 단위, ckupRcdSn 이 없으면 반려동물 전체 (FN-HLT-005-02·HLT-011-06·07)
 */
@Data
public class InspectionValueSearchCond {

    /** 검진기록일련번호 */
    private Long ckupRcdSn;
    /** 검사항목공통코드(HLTG12) */
    private String inspArtclCd;
}
