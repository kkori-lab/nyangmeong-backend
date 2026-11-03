package com.nyangmeong.pet.dto;

import java.util.List;

/**
 * [초대 발송 충돌 응답]
 * - 오류 응답 data : 고른 반려동물을 모두 검사해 충돌한 동물을 모아 준다 (FN-MYP-005-01)
 * - linkedPets : 초대받는 사람이 이미 가족으로 연동된 동물, "이미 ○○의 가족이에요"
 * - pendingPets : 같은 사람에게 대기 중인 초대가 있는 동물, "이미 초대를 보냈어요 (MM-DD에 만료돼요)"
 * - 해당 없는 목록은 빈 목록
 */
public record CaregiverInvitationConflictResponse(List<CaregiverInvitationLinkedPet> linkedPets,
        List<CaregiverInvitationPendingPet> pendingPets) {
}
