package com.nyangmeong.pet.dto;

import com.nyangmeong.pet.domain.CharacterItemSheet;

/**
 * [합성용 아이템 시트]
 * - 안 구운 아이템은 atchFileSn 없음
 */
public record CharacterSheetItem(Long itemSn, Long atchFileSn, boolean bakeFailed) {

    public static CharacterSheetItem from(CharacterItemSheet sheet) {
        return new CharacterSheetItem(sheet.getItemSn(), sheet.getAtchFileSn(), "Y".equals(sheet.getBakeFailYn()));
    }

    public static CharacterSheetItem notBaked(Long itemSn) {
        return new CharacterSheetItem(itemSn, null, false);
    }
}
