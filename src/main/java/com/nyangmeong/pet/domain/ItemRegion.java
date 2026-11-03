package com.nyangmeong.pet.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * [아이템 부위 코드]
 * - MYPG01 (ITEM.ITEM_REGN_CD, HLD_ITEM.EQUIP_REGN_CD)
 * - 신발은 굽지 않는다 (굽기 지시 없음)
 */
public enum ItemRegion {
    HEAD("MYPC0001"),
    FACE("MYPC0002"),
    NECK("MYPC0003"),
    CLOTHES("MYPC0004"),
    BAG("MYPC0005"),
    SHOES("MYPC0006");

    private final String code;

    ItemRegion(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    public static Optional<ItemRegion> from(String code) {
        return Arrays.stream(values()).filter(region -> region.is(code)).findFirst();
    }
}
