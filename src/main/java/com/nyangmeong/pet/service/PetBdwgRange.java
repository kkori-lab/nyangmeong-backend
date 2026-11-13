package com.nyangmeong.pet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * [반려동물 정상 체중 범위]
 * - 기준 체중 × 0.9 ~ × 1.1, 소수 둘째 자리 반올림(HALF_UP)
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
     * 최대 체중 상한(kg) 자리. 200kg 상한은 회의 확정 후 적용 예정이라 지금은 쓰지 않는다
     */
    public static final BigDecimal MAX_BDWG_LIMIT = null;

    /**
     * 기준 체중으로 정상 범위를 계산한다
     * - 최소는 0.01 이상, 최대는 최소 + 0.01 이상으로 보정한다 (체중이 아주 작을 때 CK_PET_WGHT_RANGE 를 지키기 위함)
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
        BigDecimal minMax = min.add(STEP);
        if (max.compareTo(minMax) < 0) {
            max = minMax;
        }
        return new PetBdwgRange(min, max);
    }
}
