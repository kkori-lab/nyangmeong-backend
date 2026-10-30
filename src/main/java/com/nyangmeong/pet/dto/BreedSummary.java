package com.nyangmeong.pet.dto;

import com.nyangmeong.pet.domain.Breed;

/**
 * [품종 목록 항목]
 */
public record BreedSummary(Long vrtySn, String vrtyCd, String vrtyNm, String petSeCd, Integer sortSeq) {

    public static BreedSummary from(Breed breed) {
        return new BreedSummary(breed.getVrtySn(), breed.getVrtyCd(), breed.getVrtyNm(), breed.getPetSeCd(),
                breed.getSortSeq());
    }
}
