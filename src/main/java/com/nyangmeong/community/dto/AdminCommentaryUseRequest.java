package com.nyangmeong.community.dto;

import jakarta.validation.constraints.NotNull;

/**
 * [관리자 AI 코멘트 사용 여부 변경 요청]
 * - FN-ADM-023-06 : 끄면 사용자 화면은 운영 설정 기본 문구
 */
public record AdminCommentaryUseRequest(@NotNull Boolean useYn) {
}
