package com.nyangmeong.pet.dto;

import java.util.List;

import com.nyangmeong.pet.domain.PixelCharacter;

/**
 * [합성용 시트 정보]
 * - 시트 파일은 번호로 따로 내려받음 (FN-MYP-010-13, FN-MYP-011-01)
 */
public record CharacterSheetsResponse(String petSeCd, Long sheetFileSn, Double scale, Double originX, Double originY,
        List<CharacterSheetItem> items) {

    public static CharacterSheetsResponse of(String petSeCd, PixelCharacter character, List<CharacterSheetItem> items) {
        return new CharacterSheetsResponse(petSeCd, character.getBodySheetFileSn(), character.getSheetScale(),
                character.getSheetOriginX(), character.getSheetOriginY(), items);
    }
}
