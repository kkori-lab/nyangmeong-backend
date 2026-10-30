package com.nyangmeong.global.code;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.external.RegionResolver;

/**
 * [검사 - 좌표로 전북 지역 찾기]
 */
class RegionServiceTest {

    private static final BigDecimal LAT = new BigDecimal("35.8242");
    private static final BigDecimal LOT = new BigDecimal("127.148");

    private final RegionMapper regionMapper = mock(RegionMapper.class);
    private final RegionResolver regionResolver = mock(RegionResolver.class);
    private final RegionService service = new RegionService(regionMapper, regionResolver);

    @Test
    void 읍면동_코드가_지역_표에_있으면_그_지역() {
        when(regionResolver.resolveAdmdstCd(LAT, LOT)).thenReturn("5211110100");
        when(regionMapper.selectRgnSnByAdmdstCd("5211110100")).thenReturn(30L);

        assertThat(service.findJeonbukRgnSn(LAT, LOT)).isEqualTo(30L);
    }

    @Test
    void 읍면동이_표에_없으면_시군구로_올려_찾는다() {
        when(regionResolver.resolveAdmdstCd(LAT, LOT)).thenReturn("5211110100");
        when(regionMapper.selectRgnSnByAdmdstCd("5211110100")).thenReturn(null);
        when(regionMapper.selectRgnSnByAdmdstCd("5211100000")).thenReturn(20L);

        assertThat(service.findJeonbukRgnSn(LAT, LOT)).isEqualTo(20L);
    }

    @Test
    void 전북_밖이거나_코드를_못_찾으면_null이고_표를_조회하지_않는다() {
        when(regionResolver.resolveAdmdstCd(LAT, LOT)).thenReturn("1100000000");
        assertThat(service.findJeonbukRgnSn(LAT, LOT)).isNull();

        when(regionResolver.resolveAdmdstCd(LAT, LOT)).thenReturn(null);
        assertThat(service.findJeonbukRgnSn(LAT, LOT)).isNull();

        verify(regionMapper, never()).selectRgnSnByAdmdstCd(any());
    }

    @Test
    void 지역_표에_전북이_없으면_null() {
        when(regionResolver.resolveAdmdstCd(LAT, LOT)).thenReturn("5211000000");
        when(regionMapper.selectRgnSnByAdmdstCd(any())).thenReturn(null);

        assertThat(service.findJeonbukRgnSn(LAT, LOT)).isNull();
    }
}
