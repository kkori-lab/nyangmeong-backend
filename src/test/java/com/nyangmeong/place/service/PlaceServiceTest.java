package com.nyangmeong.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.external.PlaceSearchClient;
import com.nyangmeong.global.external.PlaceSearchClient.PlaceSearchPage;
import com.nyangmeong.global.external.PlaceSearchClient.PlaceSearchQuery;
import com.nyangmeong.global.external.PlaceSearchItem;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;
import com.nyangmeong.place.domain.Place;
import com.nyangmeong.place.dto.PlaceSearchCond;
import com.nyangmeong.place.dto.PlaceSummary;
import com.nyangmeong.place.mapper.PlaceMapper;

/**
 * [검사 - 장소 검색]
 */
class PlaceServiceTest {

    private static final Long MBR = 1L;
    private static final String HOSPITAL = "PLCC0001";
    private static final String PET_HOSPITAL = "가정,생활 > 반려동물 > 동물병원";

    private final PlaceSearchClient placeSearchClient = mock(PlaceSearchClient.class);
    private final PlaceMapper placeMapper = mock(PlaceMapper.class);
    private final PlaceService service = new PlaceService(placeSearchClient, placeMapper);

    private PlaceSearchCond cond(String plcClsfCd, String keyword) {
        PlaceSearchCond cond = new PlaceSearchCond();
        cond.setPlcClsfCd(plcClsfCd);
        cond.setKeyword(keyword);
        cond.setSwLat(new BigDecimal("35.70"));
        cond.setSwLot(new BigDecimal("127.00"));
        cond.setNeLat(new BigDecimal("35.95"));
        cond.setNeLot(new BigDecimal("127.25"));
        cond.setCntrLat(new BigDecimal("35.8242"));
        cond.setCntrLot(new BigDecimal("127.1480"));
        return cond;
    }

    private PlaceSearchItem item(String id, String name, String categoryName) {
        return new PlaceSearchItem(id, name, "전북 전주시", "063-000-0000", "http://place.map.kakao.com/" + id,
                new BigDecimal("35.8242"), new BigDecimal("127.1480"), categoryName);
    }

    private PlaceSummary saved(String kakaoPlcId, Long plcSn, String name, Long plcBkmkSn) {
        PlaceSummary summary = new PlaceSummary();
        summary.setPlcSn(plcSn);
        summary.setKakaoPlcId(kakaoPlcId);
        summary.setPlcClsfCd(HOSPITAL);
        summary.setPlcConm(name);
        summary.setPlcLat(new BigDecimal("35.8342"));
        summary.setPlcLot(new BigDecimal("127.1480"));
        summary.setPlcBkmkSn(plcBkmkSn);
        return summary;
    }

    private PlaceSearchQuery lastQuery() {
        ArgumentCaptor<PlaceSearchQuery> captor = ArgumentCaptor.forClass(PlaceSearchQuery.class);
        verify(placeSearchClient).search(captor.capture());
        return captor.getValue();
    }

    private void assertError(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run).isInstanceOf(CustomException.class).hasFieldOrPropertyWithValue("errorCode",
                code);
    }

    @Test
    void 검색어가_없으면_카테고리_단어로_검색한다() {
        when(placeSearchClient.search(any())).thenReturn(new PlaceSearchPage(List.of(), 0, true));

        service.search(MBR, cond(HOSPITAL, null), new PageQuery(null, null, null));

        assertThat(lastQuery().query()).isEqualTo("동물병원");
    }

    @Test
    void 검색어에_카테고리_단어를_붙인다() {
        when(placeSearchClient.search(any())).thenReturn(new PlaceSearchPage(List.of(), 0, true));

        service.search(MBR, cond(HOSPITAL, " 전주 "), new PageQuery(null, null, null));
        assertThat(lastQuery().query()).isEqualTo("전주 동물병원");
    }

    @Test
    void 검색어에_카테고리_단어가_있으면_그대로_검색한다() {
        when(placeSearchClient.search(any())).thenReturn(new PlaceSearchPage(List.of(), 0, true));

        service.search(MBR, cond(HOSPITAL, "전주 동물병원"), new PageQuery(null, null, null));

        assertThat(lastQuery().query()).isEqualTo("전주 동물병원");
    }

    @Test
    void 반려동물_카테고리가_아닌_업체는_저장하지_않고_거른다() {
        when(placeSearchClient.search(any())).thenReturn(new PlaceSearchPage(
                List.of(item("1", "온누리동물병원", PET_HOSPITAL), item("2", "사람병원", "의료,건강 > 병원")), 2, true));
        when(placeMapper.selectByKakaoPlcIds(any(), anyList()))
                .thenReturn(List.of(saved("1", 11L, "온누리동물병원", null)));

        PageResponse<PlaceSummary> res = service.search(MBR, cond(HOSPITAL, null), new PageQuery(null, null, null));

        ArgumentCaptor<List<Place>> placeCaptor = ArgumentCaptor.forClass(List.class);
        verify(placeMapper).upsertKakaoPlaces(placeCaptor.capture());
        assertThat(placeCaptor.getValue()).extracting(Place::getKakaoPlcId).containsExactly("1");
        assertThat(placeCaptor.getValue().get(0).getPlcClsfCd()).isEqualTo(HOSPITAL);
        assertThat(placeCaptor.getValue().get(0).getPlcSrcCd()).isEqualTo("PLCC0008");
        assertThat(res.content()).extracting(PlaceSummary::getPlcSn).containsExactly(11L);
    }

    @Test
    void 카카오가_빈_문자열로_준_전화번호_주소_URL은_null로_저장한다() {
        PlaceSearchItem blank = new PlaceSearchItem("1", "전북동물병원", " ", "", "",
                new BigDecimal("35.8242"), new BigDecimal("127.1480"), PET_HOSPITAL);
        when(placeSearchClient.search(any())).thenReturn(new PlaceSearchPage(List.of(blank), 1, true));
        when(placeMapper.selectByKakaoPlcIds(any(), anyList()))
                .thenReturn(List.of(saved("1", 11L, "전북동물병원", null)));

        service.search(MBR, cond(HOSPITAL, null), new PageQuery(null, null, null));

        ArgumentCaptor<List<Place>> placeCaptor = ArgumentCaptor.forClass(List.class);
        verify(placeMapper).upsertKakaoPlaces(placeCaptor.capture());
        Place place = placeCaptor.getValue().get(0);
        assertThat(place.getRprsTelno()).isNull();
        assertThat(place.getPlcAddr()).isNull();
        assertThat(place.getKakaoUrlPath()).isNull();
    }

    @Test
    void 저장한_뒤_장소_번호와_즐겨찾기_번호를_붙이고_카카오_순서를_지킨다() {
        when(placeSearchClient.search(any())).thenReturn(new PlaceSearchPage(
                List.of(item("1", "가병원", PET_HOSPITAL), item("2", "나병원", PET_HOSPITAL)), 30, false));
        when(placeMapper.selectByKakaoPlcIds(any(), anyList()))
                .thenReturn(List.of(saved("2", 12L, "나병원", 5L), saved("1", 11L, "가병원", null)));

        PageResponse<PlaceSummary> res = service.search(MBR, cond(HOSPITAL, null), new PageQuery(null, null, null));

        assertThat(res.content()).extracting(PlaceSummary::getPlcSn).containsExactly(11L, 12L);
        assertThat(res.content().get(1).getPlcBkmkSn()).isEqualTo(5L);
        assertThat(res.content().get(0).getPlcBkmkSn()).isNull();
        assertThat(res.totalElements()).isEqualTo(30);
        assertThat(res.totalPages()).isEqualTo(2);
        assertThat(res.size()).isEqualTo(15);
        assertThat(res.hasNext()).isTrue();
    }

    @Test
    void 관리자가_숨긴_업체는_결과에서_빠진다() {
        when(placeSearchClient.search(any())).thenReturn(new PlaceSearchPage(
                List.of(item("1", "가병원", PET_HOSPITAL), item("2", "숨긴병원", PET_HOSPITAL)), 2, true));
        when(placeMapper.selectByKakaoPlcIds(any(), anyList()))
                .thenReturn(List.of(saved("1", 11L, "가병원", null)));

        PageResponse<PlaceSummary> res = service.search(MBR, cond(HOSPITAL, null), new PageQuery(null, null, null));

        assertThat(res.content()).extracting(PlaceSummary::getKakaoPlcId).containsExactly("1");
    }

    @Test
    void 거리는_지도_중앙에서_미터로_계산한다() {
        when(placeSearchClient.search(any())).thenReturn(
                new PlaceSearchPage(List.of(item("1", "가병원", PET_HOSPITAL)), 1, true));
        when(placeMapper.selectByKakaoPlcIds(any(), anyList()))
                .thenReturn(List.of(saved("1", 11L, "가병원", null)));

        PageResponse<PlaceSummary> res = service.search(MBR, cond(HOSPITAL, null), new PageQuery(null, null, null));

        /** 위도 0.01도 차이 = 약 1,112m */
        assertThat(res.content().get(0).getDistance()).isBetween(1100, 1125);
    }

    @Test
    void 거리순은_카카오_거리순으로_요청한다() {
        when(placeSearchClient.search(any())).thenReturn(new PlaceSearchPage(List.of(), 0, true));

        service.search(MBR, cond(HOSPITAL, null), new PageQuery(null, null, "DISTANCE"));

        assertThat(lastQuery().distanceFirst()).isTrue();
    }

    @Test
    void 이름순은_세_쪽을_모두_받아_가나다순으로_정렬한_뒤_쪽을_자른다() {
        when(placeSearchClient.search(any())).thenReturn(
                new PlaceSearchPage(List.of(item("1", "다병원", PET_HOSPITAL), item("2", "가병원", PET_HOSPITAL)), 4, false),
                new PlaceSearchPage(List.of(item("3", "나병원", PET_HOSPITAL), item("4", "라병원", PET_HOSPITAL)), 4, true));
        when(placeMapper.selectByKakaoPlcIds(any(), anyList())).thenReturn(List.of(
                saved("1", 11L, "다병원", null), saved("2", 12L, "가병원", null),
                saved("3", 13L, "나병원", null), saved("4", 14L, "라병원", null)));

        PageResponse<PlaceSummary> res = service.search(MBR, cond(HOSPITAL, null), new PageQuery(1, null, "NAME"));

        assertThat(res.content()).extracting(PlaceSummary::getPlcConm).containsExactly("가병원", "나병원", "다병원", "라병원");
        assertThat(res.totalElements()).isEqualTo(4);
        assertThat(res.hasNext()).isFalse();
    }

    @Test
    void 이름순_두_번째_쪽은_15건_뒤부터_준다() {
        List<PlaceSearchItem> items = new java.util.ArrayList<>();
        List<PlaceSummary> savedRows = new java.util.ArrayList<>();
        for (int i = 1; i <= 17; i++) {
            String name = String.format("병원%02d", i);
            items.add(item(String.valueOf(i), name, PET_HOSPITAL));
            savedRows.add(saved(String.valueOf(i), (long) i, name, null));
        }
        when(placeSearchClient.search(any())).thenReturn(new PlaceSearchPage(items, 17, true));
        when(placeMapper.selectByKakaoPlcIds(any(), anyList())).thenReturn(savedRows);

        PageResponse<PlaceSummary> res = service.search(MBR, cond(HOSPITAL, null), new PageQuery(2, null, "NAME"));

        assertThat(res.content()).extracting(PlaceSummary::getPlcConm).containsExactly("병원16", "병원17");
        assertThat(res.page()).isEqualTo(2);
        assertThat(res.hasNext()).isFalse();
    }

    @Test
    void 세_쪽을_넘기면_카카오를_부르지_않고_빈_결과다() {
        PageResponse<PlaceSummary> res = service.search(MBR, cond(HOSPITAL, null), new PageQuery(4, null, null));

        assertThat(res.content()).isEmpty();
        assertThat(res.hasNext()).isFalse();
        verify(placeSearchClient, never()).search(any());
    }

    @Test
    void 결과가_없으면_저장하지_않고_빈_목록이다() {
        when(placeSearchClient.search(any())).thenReturn(new PlaceSearchPage(List.of(), 0, true));

        PageResponse<PlaceSummary> res = service.search(MBR, cond(HOSPITAL, "없는곳"), new PageQuery(null, null, null));

        assertThat(res.content()).isEmpty();
        verify(placeMapper, never()).upsertKakaoPlaces(anyList());
    }

    @Test
    void 없는_카테고리나_정렬은_400이다() {
        assertError(() -> service.search(MBR, cond("XXXX", null), new PageQuery(null, null, null)),
                ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.search(MBR, cond(HOSPITAL, null), new PageQuery(null, null, "RANDOM")),
                ErrorCode.INVALID_INPUT_VALUE);
        verify(placeSearchClient, never()).search(any());
    }

    @Test
    void 카카오가_실패하면_502를_그대로_올린다() {
        when(placeSearchClient.search(any())).thenThrow(new CustomException(ErrorCode.EXTERNAL_API_ERROR));

        assertError(() -> service.search(MBR, cond(HOSPITAL, null), new PageQuery(null, null, null)),
                ErrorCode.EXTERNAL_API_ERROR);
        verify(placeMapper, never()).upsertKakaoPlaces(anyList());
    }
}
