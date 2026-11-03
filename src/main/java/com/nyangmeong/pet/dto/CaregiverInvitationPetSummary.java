package com.nyangmeong.pet.dto;

import lombok.Data;

/**
 * [초대 대상 반려동물 항목]
 * - CaregiverInvitationResponse.pets 와 CaregiverInvitationSummary.pets
 * - invtnSttsCd : 반려동물별 상태 (일부 수락 시 수락·거절이 갈림)
 * - vrtyNm : 목록에서 고른 품종명, vrtyDrctInptNm : 직접 입력한 품종명 (합치지 않고 따로 내린다)
 */
@Data
public class CaregiverInvitationPetSummary {
    private Long petSn;
    private String petNm;
    private String petSeCd;
    private String vrtyNm;
    private String vrtyDrctInptNm;
    private String invtnSttsCd;
}
