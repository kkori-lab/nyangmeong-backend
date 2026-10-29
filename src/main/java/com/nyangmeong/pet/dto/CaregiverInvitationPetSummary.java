package com.nyangmeong.pet.dto;

import lombok.Data;

/**
 * [초대 대상 반려동물 항목]
 * - CaregiverInvitationResponse.pets 와 CaregiverInvitationSummary.pets
 * - invtnSttsCd : 반려동물별 상태 (일부 수락 시 수락·거절이 갈림)
 */
@Data
public class CaregiverInvitationPetSummary {
    private Long petSn;
    private String petNm;
    private String petSeCd;
    private String invtnSttsCd;
}
