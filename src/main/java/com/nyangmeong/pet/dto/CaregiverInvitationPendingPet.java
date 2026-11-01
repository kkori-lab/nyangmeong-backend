package com.nyangmeong.pet.dto;

import java.time.LocalDateTime;

/**
 * [초대 충돌 - 대기 중인 초대가 있는 반려동물]
 * - CaregiverInvitationConflictResponse.pendingPets, invtnExpryDt 는 그 대기 초대의 만료일시
 */
public record CaregiverInvitationPendingPet(Long petSn, String petNm, LocalDateTime invtnExpryDt) {
}
