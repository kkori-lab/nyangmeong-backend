package com.nyangmeong.community.dto;

import java.math.BigDecimal;

import com.nyangmeong.community.domain.CareRecruitmentPet;
import com.nyangmeong.community.domain.PetSize;
import com.nyangmeong.pet.domain.PetSe;

/**
 * [펫돌봄 모집 반려동물 항목]
 * - 품종명은 마스터명, 없으면 직접입력명. petSizeCd 는 강아지만 채운다
 */
public record CareRecruitmentPetItem(String petSeCd, Long vrtySn, String vrtyNm, String petSizeCd) {

    public static CareRecruitmentPetItem from(CareRecruitmentPet pet) {
        String vrtyNm = pet.getVrtySn() != null ? pet.getVrtyNm() : pet.getVrtyDrctInptNm();
        return new CareRecruitmentPetItem(pet.getPetSeCd(), pet.getVrtySn(), vrtyNm, sizeCode(pet));
    }

    private static String sizeCode(CareRecruitmentPet pet) {
        BigDecimal bdwg = pet.getPetBdwg();
        if (!PetSe.DOG.is(pet.getPetSeCd()) || bdwg == null) {
            return null;
        }
        return PetSize.fromBdwg(bdwg).getCode();
    }
}
