package com.nyangmeong.pet.dto;

/**
 * [캐릭터 생성 작업 응답]
 * - 생성 입력인 반려동물 등록 정보 포함 (FN-MYP-010-08)
 */
public record CharacterJobResponse(Long charCrtJobSn, String petSeCd, String vrtyNm, String colrNm, String chrcCn) {

    public static CharacterJobResponse of(Long charCrtJobSn, PetProfileSummary pet) {
        return new CharacterJobResponse(charCrtJobSn, pet.getPetSeCd(), pet.getVrtyNm(), pet.getColrNm(),
                pet.getChrcCn());
    }
}
