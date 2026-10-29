package com.nyangmeong.health.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/**
 * [메인 표시 항목 전체 저장 요청]
 * - 목록 순서 = 메인 정렬 순서, 최대 6개는 서비스에서 검증
 */
public record CareItemExposureSaveRequest(@NotNull List<Long> careArtclSns) {
}
