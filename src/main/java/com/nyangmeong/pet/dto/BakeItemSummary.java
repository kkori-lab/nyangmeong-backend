package com.nyangmeong.pet.dto;

import lombok.Data;

/**
 * [굽기 대상 아이템]
 * - FN-MYP-011-01, FN-ADM-070-02
 * - regnCd 부위(MYPG01), bakeHint 영어 한 문장 굽기 지시 (그림 참고 아이템은 없을 수 있음)
 * - bakeImgRefYn true 면 아이템 그림(atchFileSn)을 참고로 굽는다
 */
@Data
public class BakeItemSummary {
    private Long itemSn;
    private String regnCd;
    private String bakeHint;
    private Boolean bakeImgRefYn;
    private Long atchFileSn;
}
