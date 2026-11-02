package com.nyangmeong.pet.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [캐릭터 아이템 착용 시트 VO]
 * - CHAR_ITEM_SHEET 1:1
 * - 굽기 실패면 ATCH_FILE_SN 없음
 */
@Data
public class CharacterItemSheet {
    private Long charItemSheetSn;
    private Long pixelCharSn;
    private Long itemSn;
    private Long atchFileSn;
    private String bakeFailYn;
    private String delYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
}
