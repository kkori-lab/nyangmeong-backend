package com.nyangmeong.pet.dto;

import com.nyangmeong.pet.domain.Breed;

/**
 * [관리자 품종 목록 항목]
 * - 사용 안 함 품종 포함 (FN-ADM-101-01)
 */
public record AdminBreedSummary(Long vrtySn, String vrtyCd, String vrtyNm, String petSeCd, Integer sortSeq,
        boolean useYn) {

    public static AdminBreedSummary from(Breed breed) {
        return new AdminBreedSummary(breed.getVrtySn(), breed.getVrtyCd(), breed.getVrtyNm(), breed.getPetSeCd(),
                breed.getSortSeq(), "Y".equals(breed.getUseYn()));
    }
}
