package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * [PetBdwgRange 단위 테스트]
 * - 기준 체중 × 0.9 ~ × 1.1, 소수 둘째 자리 HALF_UP, 아주 작은 체중 보정
 */
class PetBdwgRangeTest {

    private static PetBdwgRange of(String weight) {
        return PetBdwgRange.of(new BigDecimal(weight));
    }

    @Test
    void 기준_체중의_90퍼센트와_110퍼센트를_돌려준다() {
        PetBdwgRange range = of("4.2");

        assertThat(range.min()).isEqualByComparingTo("3.78");
        assertThat(range.max()).isEqualByComparingTo("4.62");
    }

    @Test
    void 소수_둘째_자리로_돌려준다() {
        PetBdwgRange range = of("10");

        assertThat(range.min().scale()).isEqualTo(2);
        assertThat(range.max().scale()).isEqualTo(2);
        assertThat(range.min()).isEqualByComparingTo("9.00");
        assertThat(range.max()).isEqualByComparingTo("11.00");
    }

    @Test
    void 셋째_자리가_5면_올린다() {
        /** 4.25 × 0.9 = 3.825 -> 3.83, 4.25 × 1.1 = 4.675 -> 4.68 */
        PetBdwgRange range = of("4.25");

        assertThat(range.min()).isEqualByComparingTo("3.83");
        assertThat(range.max()).isEqualByComparingTo("4.68");
    }

    @Test
    void 아주_작은_체중은_최소가_0_01_이상이다() {
        PetBdwgRange range = of("0.001");

        assertThat(range.min()).isEqualByComparingTo("0.01");
        assertThat(range.max()).isEqualByComparingTo("0.02");
    }

    @Test
    void 최대가_최소와_같아지면_최소_더하기_0_01로_보정한다() {
        /** 0.04 × 0.9 = 0.036 -> 0.04, 0.04 × 1.1 = 0.044 -> 0.04 */
        PetBdwgRange range = of("0.04");

        assertThat(range.min()).isEqualByComparingTo("0.04");
        assertThat(range.max()).isEqualByComparingTo("0.05");
    }

    @Test
    void 보정이_없는_체중에서는_최소가_최대보다_작다() {
        for (String weight : new String[] { "0.06", "0.5", "3", "25.5", "99999.999" }) {
            PetBdwgRange range = of(weight);
            assertThat(range.min()).isGreaterThan(BigDecimal.ZERO);
            assertThat(range.min()).isLessThan(range.max());
        }
    }

    @Test
    void 최대_체중_상한은_아직_적용하지_않는다() {
        assertThat(PetBdwgRange.MAX_BDWG_LIMIT).isNull();
        assertThat(of("300").max()).isEqualByComparingTo("330.00");
    }

    @Test
    void 체중이_없거나_0_이하면_예외() {
        assertThatThrownBy(() -> PetBdwgRange.of(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> of("0")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> of("-1")).isInstanceOf(IllegalArgumentException.class);
    }
}
