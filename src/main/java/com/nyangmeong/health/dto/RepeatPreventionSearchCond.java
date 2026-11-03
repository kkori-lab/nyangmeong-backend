package com.nyangmeong.health.dto;

import lombok.Data;

/**
 * [반복 예방 목록 검색 조건]
 * - 검진기록 1건 단위, ckupRcdSn 이 없으면 반려동물 전체 (FN-HLT-005-05·HLT-011-03)
 */
@Data
public class RepeatPreventionSearchCond {

    /** 검진기록일련번호 */
    private Long ckupRcdSn;
}
