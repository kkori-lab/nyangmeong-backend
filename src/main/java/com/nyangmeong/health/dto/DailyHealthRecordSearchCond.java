package com.nyangmeong.health.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * [일상 건강기록 목록 검색 조건]
 * - 기간은 fromYmd 00:00 이상, toYmd 다음 날 00:00 미만 (FN-HLT-002-09·10·12·13)
 * - size를 주면 조건에 맞는 최신 N건
 */
@Data
public class DailyHealthRecordSearchCond {

    /** 반려동물일련번호 (경로에서 채움) */
    private Long petSn;
    /** 일상기록지표공통코드(HLTG09) */
    private String dailyRcdIdctCd;
    /** 조회 시작일 */
    private LocalDate fromYmd;
    /** 조회 종료일 */
    private LocalDate toYmd;
    /** 최신 N건 */
    @Min(1)
    @Max(100)
    private Integer size;
}
