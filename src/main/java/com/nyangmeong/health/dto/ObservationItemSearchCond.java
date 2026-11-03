package com.nyangmeong.health.dto;

import lombok.Data;

/**
 * [관찰 항목 목록 검색 조건]
 * - 상태를 주면 그 상태의 항목만 (FN-HLT-002-15)
 */
@Data
public class ObservationItemSearchCond {

    /** 반려동물일련번호 (경로에서 채움) */
    private Long petSn;
    /** 관찰상태공통코드(HLTG18) */
    private String obsrvSttsCd;
}
