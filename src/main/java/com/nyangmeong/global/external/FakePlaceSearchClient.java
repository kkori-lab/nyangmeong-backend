package com.nyangmeong.global.external;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * [가짜 장소 검색]
 * - 카카오 로컬 API 연결 전 개발용 : 전주 고정 업체를 검색어·범위로 거름
 * - 검색어는 공백으로 나눈 단어가 이름·카테고리·주소에 모두 들어 있어야 일치
 */
@Component
@ConditionalOnExpression("!'${app.kakao-local.client:fake}'.equals('kakao')")
public class FakePlaceSearchClient implements PlaceSearchClient {

    private static final String HOSPITAL = "가정,생활 > 반려동물 > 동물병원";
    private static final String GROOMING = "가정,생활 > 반려동물 > 애견미용";
    private static final String SUPPLIES = "가정,생활 > 반려동물 > 애견용품";
    private static final String COMPANION = "음식점 > 카페 > 애견동반카페";
    private static final String HOTEL = "가정,생활 > 반려동물 > 애견호텔";

    private static final List<PlaceSearchItem> PLACES = List.of(
            item("fake-1", "전북동물의료센터", "전북 전주시 덕진구 번영로 1", "063-247-0001", HOSPITAL, "35.8500", "127.1300"),
            item("fake-2", "온누리동물병원", "전북 전주시 완산구 효자로 2", "063-245-0002", HOSPITAL, "35.8050", "127.1100"),
            item("fake-3", "덕진24시동물병원", "전북 전주시 덕진구 백제대로 3", "063-246-0003", HOSPITAL, "35.8400", "127.1200"),
            item("fake-4", "해피펫미용실", "전북 전주시 완산구 서신로 4", "063-251-0004", GROOMING, "35.8200", "127.1200"),
            item("fake-5", "멍멍살롱", "전북 전주시 덕진구 기린대로 5", "063-252-0005", GROOMING, "35.8300", "127.1400"),
            item("fake-6", "펫프렌즈 전주점", "전북 전주시 완산구 천잠로 6", "063-253-0006", SUPPLIES, "35.8100", "127.1500"),
            item("fake-7", "도그앤캣마트", "전북 전주시 덕진구 호성로 7", "063-254-0007", SUPPLIES, "35.8450", "127.1100"),
            item("fake-8", "댕댕카페 한옥마을점", "전북 전주시 완산구 태조로 8", "063-255-0008", COMPANION, "35.8150", "127.1530"),
            item("fake-9", "멍냥호텔", "전북 전주시 덕진구 동부대로 9", "063-256-0009", HOTEL, "35.8350", "127.1700"),
            item("fake-10", "서울동물병원", "서울 강남구 테헤란로 10", "02-555-0010", HOSPITAL, "37.5000", "127.0300"));

    @Override
    public PlaceSearchPage search(PlaceSearchQuery query) {
        List<PlaceSearchItem> matched = new ArrayList<>();
        for (PlaceSearchItem place : PLACES) {
            if (isInRect(place, query) && isMatched(place, query.query())) {
                matched.add(place);
            }
        }
        if (query.distanceFirst() && query.hasArea()) {
            matched.sort(Comparator.comparingDouble(place -> squaredGap(place, query)));
        }
        int from = (query.page() - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, matched.size());
        List<PlaceSearchItem> items = from >= matched.size() ? List.of() : matched.subList(from, to);
        return new PlaceSearchPage(items, matched.size(), to >= matched.size());
    }

    /**
     * 지도 범위 안 여부 : 좌표 없는 검색은 전체
     */
    private boolean isInRect(PlaceSearchItem place, PlaceSearchQuery query) {
        if (!query.hasArea()) {
            return true;
        }
        return place.plcLat().compareTo(query.swLat()) >= 0 && place.plcLat().compareTo(query.neLat()) <= 0
                && place.plcLot().compareTo(query.swLot()) >= 0 && place.plcLot().compareTo(query.neLot()) <= 0;
    }

    /**
     * 검색어 단어가 모두 들어 있는지
     */
    private boolean isMatched(PlaceSearchItem place, String query) {
        String text = place.plcConm() + " " + place.categoryName() + " " + place.plcAddr();
        for (String word : query.trim().split("\s+")) {
            if (!word.isEmpty() && !text.contains(word)) {
                return false;
            }
        }
        return true;
    }

    private double squaredGap(PlaceSearchItem place, PlaceSearchQuery query) {
        double lat = place.plcLat().subtract(query.cntrLat()).doubleValue();
        double lot = place.plcLot().subtract(query.cntrLot()).doubleValue();
        return lat * lat + lot * lot;
    }

    private static PlaceSearchItem item(String id, String name, String addr, String telno, String categoryName,
            String lat, String lot) {
        return new PlaceSearchItem(id, name, addr, telno, "http://place.map.kakao.com/" + id,
                new BigDecimal(lat), new BigDecimal(lot), categoryName);
    }
}
