package com.nyangmeong.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [AI 건강분석 리포트 저장본]
 */
public record HealthReportResponse(String rptTtl, List<String> tips, String inputHash, LocalDateTime crtDt) {
}
