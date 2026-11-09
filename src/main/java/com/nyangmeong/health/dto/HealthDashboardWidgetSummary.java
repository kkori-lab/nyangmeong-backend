package com.nyangmeong.health.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

/**
 * [건강기록 대시보드 항목 요약]
 * - GET·PUT /api/members/me/dashboard-widgets 의 widgets 한 행, 보일 순서대로
 */
@Data
public class HealthDashboardWidgetSummary {

    /** 대시보드항목공통코드(HLTG25) */
    private String dshbdWdgtCd;
    /** 항목 이름 */
    private String dshbdWdgtCdNm;
    /** 표시여부 */
    private boolean indctYn;
    /** 정렬순서 */
    private Integer sortSeq;
    /** 저장된 행 여부 : 응답에는 싣지 않고 savedYn 계산에만 씀 */
    @JsonIgnore
    private boolean savedYn;
}
