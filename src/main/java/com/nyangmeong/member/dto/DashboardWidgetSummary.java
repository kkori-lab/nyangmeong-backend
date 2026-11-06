package com.nyangmeong.member.dto;

import lombok.Data;

/**
 * [메인 위젯 순서 항목]
 * - GET·PUT /api/members/me/dashboard-widgets (FN-MAN-006·007), 표시할 순서대로
 */
@Data
public class DashboardWidgetSummary {
    private String dshbdWdgtCd;
    private String dshbdWdgtCdNm;
    private Integer sortSeq;
}
