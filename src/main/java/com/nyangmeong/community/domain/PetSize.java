package com.nyangmeong.community.domain;

import java.math.BigDecimal;

/**
 * [반려견 크기]
 * - 소형 10kg 미만, 중형 10kg 이상 25kg 미만, 대형 25kg 이상
 * - RCRT_PET_KND.PET_BDWG 에는 크기별 대표 체중을 저장 (docs/api/issues.md 커뮤니티 11번)
 */
public enum PetSize {
    SMALL("SMALL", new BigDecimal("5"), BigDecimal.ZERO, new BigDecimal("10")),
    MEDIUM("MEDIUM", new BigDecimal("15"), new BigDecimal("10"), new BigDecimal("25")),
    LARGE("LARGE", new BigDecimal("30"), new BigDecimal("25"), null);

    private final String code;
    private final BigDecimal bdwg;
    private final BigDecimal minBdwg;
    private final BigDecimal maxBdwg;

    PetSize(String code, BigDecimal bdwg, BigDecimal minBdwg, BigDecimal maxBdwg) {
        this.code = code;
        this.bdwg = bdwg;
        this.minBdwg = minBdwg;
        this.maxBdwg = maxBdwg;
    }

    public String getCode() {
        return code;
    }

    /** 저장하는 대표 체중 */
    public BigDecimal getBdwg() {
        return bdwg;
    }

    /** 구간 하한(포함) */
    public BigDecimal getMinBdwg() {
        return minBdwg;
    }

    /** 구간 상한(미포함), 대형은 null */
    public BigDecimal getMaxBdwg() {
        return maxBdwg;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 코드로 크기 조회 (없는 코드면 null)
     */
    public static PetSize from(String code) {
        for (PetSize size : values()) {
            if (size.is(code)) {
                return size;
            }
        }
        return null;
    }

    /**
     * 저장된 체중의 크기
     */
    public static PetSize fromBdwg(BigDecimal bdwg) {
        for (PetSize size : values()) {
            if (bdwg.compareTo(size.minBdwg) >= 0 && (size.maxBdwg == null || bdwg.compareTo(size.maxBdwg) < 0)) {
                return size;
            }
        }
        return SMALL;
    }
}
