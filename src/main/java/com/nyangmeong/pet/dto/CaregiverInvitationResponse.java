package com.nyangmeong.pet.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [받은 초대 상세 응답]
 * - 초대한 사람·반려동물은 대기중이고 이메일이 맞을 때만 : 다른 계정에 남의 반려동물 정보 노출 방지
 */
public record CaregiverInvitationResponse(Long jntBrupInvtnSn, String sndrMbrNcnm, String invtnSttsCd,
        LocalDateTime invtnExpryDt, String invtnTrgtEmlMask, boolean emailMatched,
        List<CaregiverInvitationPetSummary> pets) {
}
