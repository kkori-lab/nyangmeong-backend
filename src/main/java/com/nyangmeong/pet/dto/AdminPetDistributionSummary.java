package com.nyangmeong.pet.dto;

import lombok.Data;

/**
 * [관리자 대시보드 반려동물 종·품종 분포]
 * - SCR-ADM-001 (FN-ADM-001-04) : 기간 무관 지금 등록된 반려동물
 */
@Data
public class AdminPetDistributionSummary {
    /** 종 이름 */
    private String petSeNm;
    /** 품종 이름 : 품종을 직접 입력한 반려동물은 "직접 입력" */
    private String vrtyNm;
    /** 마릿수 */
    private long cnt;
}
