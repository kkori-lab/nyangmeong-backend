package com.nyangmeong.pet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * [관리자 캐릭터 생성 작업 상태별 건수]
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCharacterJobStatusCount {
    /** 생성작업상태공통코드 (CHRG01) */
    private String crtJobSttsCd;
    private long cnt;
}
