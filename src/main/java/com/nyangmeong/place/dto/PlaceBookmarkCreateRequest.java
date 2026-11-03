package com.nyangmeong.place.dto;

import jakarta.validation.constraints.NotNull;

/**
 * [장소 즐겨찾기 저장 요청]
 */
public record PlaceBookmarkCreateRequest(@NotNull Long plcSn) {
}
