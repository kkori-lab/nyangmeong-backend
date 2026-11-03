package com.nyangmeong.health.dto;

import lombok.Data;

/**
 * [처방약 목록 검색 조건]
 * - 검진기록 1건 단위, ckupRcdSn 이 없으면 반려동물 전체 (FN-HLT-005-03·HLT-011-08)
 */
@Data
public class PrescriptionSearchCond {

    /** 검진기록일련번호 */
    private Long ckupRcdSn;
    /** 처방상태공통코드(HLTG06) */
    private String prscSttsCd;
}
