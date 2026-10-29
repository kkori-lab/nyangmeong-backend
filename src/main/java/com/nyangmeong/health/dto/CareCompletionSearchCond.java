package com.nyangmeong.health.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * [케어 완료 기록 검색 조건]
 */
@Data
public class CareCompletionSearchCond {

    /** 케어항목일련번호 */
    private Long careArtclSn;
    /** 시작일(포함) */
    private LocalDate fromYmd;
    /** 종료일(포함) */
    private LocalDate toYmd;
}
