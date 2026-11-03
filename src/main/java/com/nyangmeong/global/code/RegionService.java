package com.nyangmeong.global.code;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.external.RegionResolver;

import lombok.RequiredArgsConstructor;

/**
 * [지역 서비스]
 * - 지역 콤보박스 목록, 좌표로 전북 지역 번호 찾기 (FN-CMU-002-03·10)
 */
@Service
@RequiredArgsConstructor
public class RegionService {

    /** 전북특별자치도 행정구역코드 앞 2자리 */
    static final String JEONBUK_PREFIX = "52";

    private final RegionMapper regionMapper;
    private final RegionResolver regionResolver;

    /**
     * 하위 지역 목록 조회 (upRgnSn 없으면 시도)
     */
    @Transactional(readOnly = true)
    public List<RegionSummary> getRegions(Long upRgnSn) {
        return regionMapper.selectList(upRgnSn);
    }

    /**
     * 좌표의 전북 지역 번호 (전북 밖이거나 지역을 찾지 못하면 null)
     * - 읍면동 코드가 지역 표에 없으면 시군구, 시도 순으로 올려 찾는다
     */
    @Transactional(readOnly = true)
    public Long findJeonbukRgnSn(BigDecimal lat, BigDecimal lot) {
        String admdstCd = regionResolver.resolveAdmdstCd(lat, lot);
        if (admdstCd == null || admdstCd.length() != 10 || !admdstCd.startsWith(JEONBUK_PREFIX)) {
            return null;
        }
        for (String candidate : List.of(admdstCd, admdstCd.substring(0, 5) + "00000",
                admdstCd.substring(0, 2) + "00000000")) {
            Long rgnSn = regionMapper.selectRgnSnByAdmdstCd(candidate);
            if (rgnSn != null) {
                return rgnSn;
            }
        }
        return null;
    }
}
