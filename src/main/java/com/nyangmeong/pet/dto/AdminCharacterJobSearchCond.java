package com.nyangmeong.pet.dto;

import lombok.Data;

/**
 * [관리자 캐릭터 생성 현황 검색 조건]
 * - SCR-ADM-082, 조건은 선택. 상태 건수(sttsCnts)는 상태 필터와 무관
 */
@Data
public class AdminCharacterJobSearchCond {
    /** 생성작업상태공통코드 (CHRG01) */
    private String crtJobSttsCd;
}
