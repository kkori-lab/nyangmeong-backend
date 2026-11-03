package com.nyangmeong.pet.dto;

import lombok.Data;

/**
 * [관리자 아이템 목록 행]
 * - FN-ADM-070-01
 * - hldMbrCnt : 보유 캐릭터 수 (캐릭터는 반려동물당 1개)
 */
@Data
public class AdminItemSummary {
    private Long itemSn;
    private String itemNm;
    private String itemRegnCd;
    private String itemRegnCdNm;
    private Long needPnt;
    private Boolean ntslYn;
    private Long sortSeq;
    private Long atchFileSn;
    private String bakeHint;
    private Long hldMbrCnt;
}
