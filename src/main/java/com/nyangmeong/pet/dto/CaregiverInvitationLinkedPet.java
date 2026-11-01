package com.nyangmeong.pet.dto;

/**
 * [초대 충돌 - 이미 가족으로 연동된 반려동물]
 * - CaregiverInvitationConflictResponse.linkedPets
 */
public record CaregiverInvitationLinkedPet(Long petSn, String petNm) {
}
