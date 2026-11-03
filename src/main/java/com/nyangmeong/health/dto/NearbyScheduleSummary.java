package com.nyangmeong.health.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * [가까운 예정 병원 일정]
 * - 검진일 앞뒤 7일 안의 검진기록 미연결 병원 일정 (FN-HLT-002-46)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearbyScheduleSummary {

    /** 일정일련번호 */
    private Long schdlSn;
    /** 일정일시 */
    private LocalDateTime schdlDt;
    /** 병원명 */
    private String hsptlNm;
}
