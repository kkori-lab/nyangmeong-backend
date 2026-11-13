package com.nyangmeong.pet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * [반려동물 정상 체중 범위]
 * - 기준 체중 × 0.9 ~ × 1.1, 소수 둘째 자리 반올림(HALF_UP), 최대 200kg(MAX_BDWG_LIMIT)에서 자른다
 * - 사용: PetBdwgRange r = PetBdwgRange.of(기준체중); r.min() r.max()
 * - 다른 도메인(건강기록 등)이 service 패키지의 이 클래스를 바로 쓴다
 * - 'Y'(사용자 직접 입력)인 반려동물의 범위는 자동으로 바꾸지 않는다. 호출하는 쪽이 PET.BDWG_RNG_DRCT_INPT_YN 을 확인한다
 */
public record PetBdwgRange(BigDecimal min, BigDecimal max) {

    private static final BigDecimal MIN_RATE = new BigDecimal("0.9");
    private static final BigDecimal MAX_RATE = new BigDecimal("1.1");
    private static final BigDecimal STEP = new BigDecimal("0.01");
    private static final int SCALE = 2;

    /**
     * 체중·최소·최대 체중 상한(kg). 회의 확정: 200kg
     */
    public static final BigDecimal MAX_BDWG_LIMIT = new BigDecimal("200.00");

    /**
     * 기준 체중으로 정상 범위를 계산한다
     * - 최소는 0.01 이상, 최대는 최소 + 0.01 이상으로 보정한다 (체중이 아주 작을 때 CK_PET_WGHT_RANGE 를 지키기 위함)
     * - 최대가 200 을 넘으면 200 으로 자르고, 최소가 200 이상이면 199.99 로 잘라 최소 < 최대를 지킨다
     */
    public static PetBdwgRange of(BigDecimal baseWeight) {
        if (baseWeight == null || baseWeight.signum() <= 0) {
            throw new IllegalArgumentException("baseWeight must be positive");
        }
        BigDecimal min = baseWeight.multiply(MIN_RATE).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal max = baseWeight.multiply(MAX_RATE).setScale(SCALE, RoundingMode.HALF_UP);
        if (min.compareTo(STEP) < 0) {
            min = STEP;
        }
        BigDecimal minCap = MAX_BDWG_LIMIT.subtract(STEP);
        if (min.compareTo(minCap) > 0) {
            min = minCap;
        }
        BigDecimal minMax = min.add(STEP);
        if (max.compareTo(minMax) < 0) {
            max = minMax;
        }
        if (max.compareTo(MAX_BDWG_LIMIT) > 0) {
            max = MAX_BDWG_LIMIT;
        }
        return new PetBdwgRange(min, max);
    }
}
