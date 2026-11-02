package com.nyangmeong.pet.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * [캐릭터 생성 실패 보고]
 * - Python AI 서버 -> Java (FN-MYP-010-12)
 */
public record CharacterFailureRequest(@NotBlank String failRsn) {
}
