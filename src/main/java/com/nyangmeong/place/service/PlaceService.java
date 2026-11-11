package com.nyangmeong.place.service;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.external.PlaceSearchClient;
import com.nyangmeong.global.external.PlaceSearchClient.PlaceSearchPage;
import com.nyangmeong.global.external.PlaceSearchClient.PlaceSearchQuery;
import com.nyangmeong.global.external.PlaceSearchItem;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.place.domain.Place;
import com.nyangmeong.place.domain.PlaceCategory;
import com.nyangmeong.place.domain.PlaceSort;
import com.nyangmeong.place.domain.PlaceSource;
import com.nyangmeong.place.dto.PlaceSearchCond;
import com.nyangmeong.place.dto.PlaceSummary;
import com.nyangmeong.place.mapper.PlaceMapper;

import lombok.RequiredArgsConstructor;

/**
 * [장소 검색 서비스]
 * - SCR-HOS-001·PLC-001~004 (FN-HOS-001-03~07·16·19·20·30)
 * - 카카오 검색 결과를 PLC 에 저장한 뒤 장소 번호·내 즐겨찾기 번호를 붙여 반환
 * - 이름순은 카카오가 지원하지 않아 전체(최대 45건)를 받아 정렬한 뒤 15건씩 자름
 */
@Service
@RequiredArgsConstructor
public class PlaceService {

    private static final int PLC_CONM_MAX_LENGTH = 200;
    private static final int PLC_ADDR_MAX_LENGTH = 200;
    private static final int RPRS_TELNO_MAX_LENGTH = 20;
    private static final int KAKAO_URL_PATH_MAX_LENGTH = 500;
    private static final double EARTH_RADIUS_METER = 6_371_000;

    private final PlaceSearchClient placeSearchClient;
    private final PlaceMapper placeMapper;

    /**
     * 장소 검색 : 외부 호출 동안 DB 연결을 잡지 않으려고 트랜잭션 없이 실행, 저장은 쿼리 한 번이라 그 자체로 원자적
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PageResponse<PlaceSummary> search(Long mbrSn, PlaceSearchCond cond, PageQuery pq) {
        PlaceCategory category = PlaceCategory.from(cond.getPlcClsfCd());
        PlaceSort sort = PlaceSort.from(pq.sort());
        validateCoordinates(cond, sort);
        int page = pq.pageOrDefault();
        String query = category.toQuery(cond.getKeyword());

        if (page > PlaceSearchClient.MAX_PAGE) {
            return emptyPage(page);
        }
        if (PlaceSort.NAME.is(sort.getCode())) {
            return searchByName(mbrSn, category, cond, query, page);
        }

        PlaceSearchPage result = placeSearchClient.search(toQuery(query, sort, cond, page));
        List<PlaceSummary> content = resolve(mbrSn, category, cond, result.items());
        boolean hasNext = !result.end() && page < PlaceSearchClient.MAX_PAGE;
        return toPage(content, page, result.totalCount(), hasNext);
    }

    /**
     * 이름순 검색 : 최대 3쪽을 모두 받아 상호명 가나다순으로 정렬한 뒤 요청한 쪽만 반환
     */
    private PageResponse<PlaceSummary> searchByName(Long mbrSn, PlaceCategory category, PlaceSearchCond cond,
            String query, int page) {
        List<PlaceSearchItem> items = new ArrayList<>();
        for (int current = 1; current <= PlaceSearchClient.MAX_PAGE; current++) {
            PlaceSearchPage result = placeSearchClient
                    .search(toQuery(query, PlaceSort.NAME, cond, current));
            items.addAll(result.items());
            if (result.end()) {
                break;
            }
        }
        List<PlaceSummary> all = new ArrayList<>(resolve(mbrSn, category, cond, items));
        all.sort(Comparator.comparing(PlaceSummary::getPlcConm, Collator.getInstance(Locale.KOREAN)));

        int from = (page - 1) * PlaceSearchClient.PAGE_SIZE;
        int to = Math.min(from + PlaceSearchClient.PAGE_SIZE, all.size());
        List<PlaceSummary> content = from >= all.size() ? List.of() : all.subList(from, to);
        return toPage(content, page, all.size(), to < all.size());
    }

    /**
     * 카카오 검색 결과를 저장하고 장소 번호·즐겨찾기 번호·거리를 붙인 목록 : 카카오 순서 유지, 숨긴 업체 제외
     */
    private List<PlaceSummary> resolve(Long mbrSn, PlaceCategory category, PlaceSearchCond cond,
            List<PlaceSearchItem> items) {
        Map<String, PlaceSearchItem> kakaoItems = new LinkedHashMap<>();
        for (PlaceSearchItem item : items) {
            if (category.matches(item.categoryName())) {
                kakaoItems.putIfAbsent(item.kakaoPlcId(), item);
            }
        }
        if (kakaoItems.isEmpty()) {
            return List.of();
        }

        placeMapper.upsertKakaoPlaces(kakaoItems.values().stream().map(item -> toPlace(category, item)).toList());
        Map<String, PlaceSummary> saved = placeMapper.selectByKakaoPlcIds(mbrSn, new ArrayList<>(kakaoItems.keySet()))
                .stream()
                .collect(Collectors.toMap(PlaceSummary::getKakaoPlcId, Function.identity()));

        List<PlaceSummary> content = new ArrayList<>();
        for (String kakaoPlcId : kakaoItems.keySet()) {
            PlaceSummary summary = saved.get(kakaoPlcId);
            if (summary != null) {
                if (cond.hasCoordinates()) {
                    summary.setDistance(distanceMeter(cond.getCntrLat(), cond.getCntrLot(), summary.getPlcLat(),
                            summary.getPlcLot()));
                }
                content.add(summary);
            }
        }
        return content;
    }

    /**
     * 좌표 6칸은 모두 있거나 모두 없어야 함, 좌표 없이 거리순 불가
     */
    private void validateCoordinates(PlaceSearchCond cond, PlaceSort sort) {
        if (cond.hasCoordinates()) {
            return;
        }
        if (!cond.hasNoCoordinates()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, missingCoordinateFields(cond));
        }
        if (PlaceSort.DISTANCE.is(sort.getCode())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "sort");
        }
    }

    /**
     * 일부만 온 좌표에서 빠진 칸 이름
     */
    private String[] missingCoordinateFields(PlaceSearchCond cond) {
        Map<String, BigDecimal> coordinates = new LinkedHashMap<>();
        coordinates.put("swLat", cond.getSwLat());
        coordinates.put("swLot", cond.getSwLot());
        coordinates.put("neLat", cond.getNeLat());
        coordinates.put("neLot", cond.getNeLot());
        coordinates.put("cntrLat", cond.getCntrLat());
        coordinates.put("cntrLot", cond.getCntrLot());
        return coordinates.entrySet().stream()
                .filter(entry -> entry.getValue() == null)
                .map(Map.Entry::getKey)
                .toArray(String[]::new);
    }

    private PlaceSearchQuery toQuery(String query, PlaceSort sort, PlaceSearchCond cond, int page) {
        return new PlaceSearchQuery(query, PlaceSort.DISTANCE.is(sort.getCode()), cond.getSwLat(), cond.getSwLot(),
                cond.getNeLat(), cond.getNeLot(), cond.getCntrLat(), cond.getCntrLot(), page);
    }

    private Place toPlace(PlaceCategory category, PlaceSearchItem item) {
        Place place = new Place();
        place.setPlcClsfCd(category.getCode());
        place.setPlcSrcCd(PlaceSource.KAKAO.getCode());
        place.setPlcConm(cut(item.plcConm(), PLC_CONM_MAX_LENGTH));
        place.setPlcAddr(cut(blankToNull(item.plcAddr()), PLC_ADDR_MAX_LENGTH));
        place.setRprsTelno(dropIfLonger(blankToNull(item.rprsTelno()), RPRS_TELNO_MAX_LENGTH));
        place.setKakaoPlcId(item.kakaoPlcId());
        place.setKakaoUrlPath(dropIfLonger(blankToNull(item.kakaoUrlPath()), KAKAO_URL_PATH_MAX_LENGTH));
        place.setPlcLat(item.plcLat());
        place.setPlcLot(item.plcLot());
        return place;
    }

    /**
     * 두 좌표 사이 거리(미터), 하버사인 공식
     */
    static int distanceMeter(BigDecimal fromLat, BigDecimal fromLot, BigDecimal toLat, BigDecimal toLot) {
        double lat1 = Math.toRadians(fromLat.doubleValue());
        double lat2 = Math.toRadians(toLat.doubleValue());
        double dLat = lat2 - lat1;
        double dLot = Math.toRadians(toLot.doubleValue() - fromLot.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLot / 2) * Math.sin(dLot / 2);
        return (int) Math.round(2 * EARTH_RADIUS_METER * Math.asin(Math.sqrt(a)));
    }

    private PageResponse<PlaceSummary> toPage(List<PlaceSummary> content, int page, int totalElements,
            boolean hasNext) {
        int totalPages = (totalElements + PlaceSearchClient.PAGE_SIZE - 1) / PlaceSearchClient.PAGE_SIZE;
        return new PageResponse<>(content, page, PlaceSearchClient.PAGE_SIZE, totalElements, totalPages, hasNext);
    }

    private PageResponse<PlaceSummary> emptyPage(int page) {
        return new PageResponse<>(List.of(), page, PlaceSearchClient.PAGE_SIZE, 0, 0, false);
    }

    /**
     * 빈 값 정리 : 카카오는 없는 전화번호를 빈 문자열로 줌
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String cut(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String dropIfLonger(String value, int maxLength) {
        return value == null || value.length() > maxLength ? null : value;
    }
}
