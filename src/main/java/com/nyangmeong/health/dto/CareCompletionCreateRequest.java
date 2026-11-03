package com.nyangmeong.health.dto;

import jakarta.validation.constraints.NotNull;

/**
 * [케어 완료 체크 요청]
 * - 완료일은 요청에 없고 서버의 오늘
 */
public record CareCompletionCreateRequest(@NotNull Long careArtclSn) {
}
