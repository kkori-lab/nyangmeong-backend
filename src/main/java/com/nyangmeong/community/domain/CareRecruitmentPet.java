package com.nyangmeong.community.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * [모집 반려동물 종류 VO]
 * - RCRT_PET_KND 1:1, vrtyNm 은 품종 마스터 조인 값(조회 때만 채움)
 */
@Data
public class CareRecruitmentPet {
    private Long rcrtPetKndSn;
    private Long petPrtcRcrtSn;
    private String petSeCd;
    private BigDecimal petBdwg;
    private Long vrtySn;
    private String vrtyDrctInptNm;
    private LocalDateTime regDt;
    private String vrtyNm;
}
