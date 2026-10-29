package com.nyangmeong.pet.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [초대대상반려동물 VO]
 * - INVTN_TRGT_PET 1:1, ACPT_YN은 응답 전 null
 */
@Data
public class InvtnTrgtPet {
    private Long invtnTrgtPetSn;
    private Long jntBrupInvtnSn;
    private Long petSn;
    private String acptYn;
    private LocalDateTime regDt;
}
