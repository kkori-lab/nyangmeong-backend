package com.nyangmeong.global.external;

import java.math.BigDecimal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * [가짜 역지오코딩]
 * - 외부 API 연결 전 개발용 : 좌표와 무관하게 app.region.client 값의 고정 결과
 */
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${app.region.client:fake}'.startsWith('fake')")
public class FakeRegionResolver implements RegionResolver {

    private final RegionProperties regionProperties;

    @Override
    public String resolveAdmdstCd(BigDecimal lat, BigDecimal lot) {
        return switch (regionProperties.getClient()) {
            case "fake-outside" -> "1100000000";
            case "fake-none" -> null;
            default -> "5211000000";
        };
    }
}
