package com.nyangmeong.pet.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [반려동물 메인 바로가기 VO]
 * - PET_MAIN_SHRTC 1:1
 */
@Data
public class PetMainShrtc {
    private Long petMainShrtcSn;
    private Long mbrSn;
    private Long petSn;
    private String shrtcCd;
    private String indctYn;
    private Integer sortSeq;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
}
