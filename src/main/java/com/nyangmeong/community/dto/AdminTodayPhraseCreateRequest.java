package com.nyangmeong.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * [관리자 오늘의 한마디 후보 문구 등록 요청]
 * - FN-ADM-023-04 : phrsCn 은 앞뒤 공백을 지운 뒤 200자 이하
 */
public record AdminTodayPhraseCreateRequest(@NotBlank String phrsCn, @NotNull Boolean useYn) {
}
