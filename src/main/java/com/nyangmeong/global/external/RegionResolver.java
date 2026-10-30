package com.nyangmeong.global.external;

import java.math.BigDecimal;

/**
 * [좌표 → 행정구역코드 변환 외부 API]
 * - 역지오코딩, 행정구역코드(10자리, RGN.ADMDST_CD 체계)를 돌려주고 찾지 못하면 null
 * - 카카오 로컬 API 연결 전까지 FakeRegionResolver
 */
public interface RegionResolver {

    /**
     * 좌표의 행정구역코드
     */
    String resolveAdmdstCd(BigDecimal lat, BigDecimal lot);
}
