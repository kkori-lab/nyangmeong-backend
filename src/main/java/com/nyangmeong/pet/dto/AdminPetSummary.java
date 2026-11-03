package com.nyangmeong.pet.dto;

import lombok.Data;

/**
 * [관리자 회원 상세 반려동물 한 줄]
 * - SCR-ADM-011, 건강 정보(알레르기·기저질환)는 응답에서 제외
 */
@Data
public class AdminPetSummary {
    /** 반려동물일련번호 */
    private Long petSn;
    /** 반려동물명 */
    private String petNm;
    /** 반려동물구분공통코드 */
    private String petSeCd;
    /** 품종명 */
    private String vrtyNm;
    /** 품종직접입력명 */
    private String vrtyDrctInptNm;
    /** 양육역할공통코드 */
    private String brupRoleCd;
    /** 연동 중 공동양육자 수 (주양육자 제외) */
    private Integer caregiverCount;
    /** 삭제여부 */
    private boolean delYn;
}
