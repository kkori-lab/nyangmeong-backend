package com.nyangmeong.pet.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [아이템]
 * - ITEM
 */
@Data
public class Item {
    private Long itemSn;
    private Long atchFileSn;
    private String itemRegnCd;
    private String itemNm;
    private Long needPnt;
    private Long sortSeq;
    private String ntslYn;
    private String bakeHint;
    private String bakeImgRefYn;
    private LocalDateTime regDt;
    private LocalDateTime mdfcnDt;
    private String rgtrId;
    private String mdfrId;
}
