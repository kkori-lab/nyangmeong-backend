package com.nyangmeong.health.dto;

import lombok.Data;

/**
 * [검진기록 첨부 목록 검색 조건]
 * - 검진기록 1건 단위, ckupRcdSn 이 없으면 반려동물 전체 (FN-HLT-005-01·HLT-011-09)
 */
@Data
public class CheckupAttachmentSearchCond {

    /** 검진기록일련번호 */
    private Long ckupRcdSn;
    /** 첨부유형공통코드(HLTG20) */
    private String atchTypeCd;
}
