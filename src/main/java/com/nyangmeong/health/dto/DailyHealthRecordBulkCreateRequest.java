package com.nyangmeong.health.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * [일상 건강기록 일괄 등록 요청]
 * - 건마다 DailyHealthRecordCreateRequest와 같은 값·검증 (FN-HLT-002-11, ※신규 정책)
 * - 건수(1~5)·같은 종류 중복은 서비스가 검사 : 칸 이름 없는 400으로 내려고 여기에 크기 검사를 두지 않음
 */
public record DailyHealthRecordBulkCreateRequest(
        List<@NotNull @Valid DailyHealthRecordCreateRequest> values) {
}
