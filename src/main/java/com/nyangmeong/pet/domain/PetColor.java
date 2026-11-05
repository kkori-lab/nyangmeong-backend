package com.nyangmeong.pet.domain;

import java.util.Set;

/**
 * [털색 코드]
 * - PETG03 (PET.COLR_CD), 직접 입력이면 COLR_DRCT_INPT_NM 필수
 * - 강아지·고양이 전용 코드는 다른 종에 쓸 수 없다. 그 밖의 코드(공통, 직접 입력)는 어느 종에서나 받는다.
 */
public enum PetColor {
    DIRECT_INPUT("PETC0017");

    /** 강아지 전용: 블랙탄·얼룩·브린들·황색·파티·세이블·멀 */
    private static final Set<String> DOG_ONLY = Set.of(
            "PETC0010", "PETC0011", "PETC0012", "PETC0028", "PETC0029", "PETC0030", "PETC0031");

    /** 고양이 전용: 고등어·치즈·턱시도·젖소·카오스·포인트 */
    private static final Set<String> CAT_ONLY = Set.of(
            "PETC0014", "PETC0015", "PETC0016", "PETC0032", "PETC0033", "PETC0034");

    private final String code;

    PetColor(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 털색 코드가 반려동물 구분에 맞는지 본다.
     */
    public static boolean fits(String petSeCd, String colrCd) {
        if (PetSe.DOG.is(petSeCd)) {
            return !CAT_ONLY.contains(colrCd);
        }
        if (PetSe.CAT.is(petSeCd)) {
            return !DOG_ONLY.contains(colrCd);
        }
        return true;
    }
}
