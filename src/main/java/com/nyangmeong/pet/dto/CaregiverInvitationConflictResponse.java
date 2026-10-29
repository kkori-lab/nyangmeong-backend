package com.nyangmeong.pet.dto;

import java.time.LocalDateTime;

/**
 * [초대 발송 충돌 응답]
 * - 오류 응답 data : "이미 ○○의 가족이에요", "MM-DD에 만료돼요" 문구용
 */
public record CaregiverInvitationConflictResponse(Long petSn, String petNm, LocalDateTime invtnExpryDt) {
}
