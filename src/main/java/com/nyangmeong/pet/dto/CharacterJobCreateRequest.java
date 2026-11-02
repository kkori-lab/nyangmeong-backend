package com.nyangmeong.pet.dto;

import jakarta.validation.constraints.NotNull;

/**
 * [캐릭터 생성 작업 요청]
 * - Python AI 서버 -> Java (FN-MYP-010-08)
 */
public record CharacterJobCreateRequest(@NotNull Long petSn) {
}
