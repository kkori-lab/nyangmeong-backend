package com.nyangmeong.global.external;

import java.math.BigDecimal;
import java.util.List;

/**
 * [장소 키워드 검색 외부 API]
 * - 카카오 로컬 API 키워드 검색, 한 쪽은 최대 15건·최대 3쪽(45건)
 * - 카카오 로컬 API 키를 받기 전까지 FakePlaceSearchClient
 */
public interface PlaceSearchClient {

    /** 카카오 쪽 크기 */
    int PAGE_SIZE = 15;

    /** 카카오 최대 쪽 수 */
    int MAX_PAGE = 3;

    /**
     * 키워드·지도 범위로 한 쪽 검색
     */
    PlaceSearchPage search(PlaceSearchQuery query);

    /**
     * 검색 조건 : 범위는 남서·북동 모서리, 중앙은 거리순 정렬 기준
     * - 좌표가 모두 null 이면 범위·중앙 없이 전국 검색 (거리순 불가)
     */
    record PlaceSearchQuery(
            String query,
            boolean distanceFirst,
            BigDecimal swLat,
            BigDecimal swLot,
            BigDecimal neLat,
            BigDecimal neLot,
            BigDecimal cntrLat,
            BigDecimal cntrLot,
            int page) {

        /**
         * 범위·중앙 좌표가 있는지
         */
        public boolean hasArea() {
            return swLat != null && swLot != null && neLat != null && neLot != null && cntrLat != null
                    && cntrLot != null;
        }
    }

    /**
     * 검색 한 쪽 : totalCount 는 카카오 pageable_count(최대 45)
     */
    record PlaceSearchPage(List<PlaceSearchItem> items, int totalCount, boolean end) {
    }
}
