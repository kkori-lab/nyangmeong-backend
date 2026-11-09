package com.nyangmeong.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.HealthDashboardItem;
import com.nyangmeong.health.dto.HealthDashboardWidgetResponse;
import com.nyangmeong.health.dto.HealthDashboardWidgetSaveRequest;
import com.nyangmeong.health.dto.HealthDashboardWidgetSaveRequest.Widget;
import com.nyangmeong.health.dto.HealthDashboardWidgetSummary;
import com.nyangmeong.health.dto.HealthRecordedItemSummary;
import com.nyangmeong.health.mapper.HealthDashboardMapper;
import com.nyangmeong.health.mapper.HealthDashboardWidgetMapper;
import com.nyangmeong.pet.dto.PetSummary;
import com.nyangmeong.pet.service.PetService;

/**
 * [검사 - 건강기록 대시보드 고른 항목]
 * - 저장 유무 구분, 순서·표시여부 저장, 모르는·중복·빠진 코드 400, 기록 있는 항목 매핑
 */
class HealthDashboardWidgetServiceTest {

    private static final Long MBR = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);

    private final HealthDashboardWidgetMapper widgetMapper = mock(HealthDashboardWidgetMapper.class);
    private final HealthDashboardMapper dashboardMapper = mock(HealthDashboardMapper.class);
    private final PetService petService = mock(PetService.class);
    private final HealthDashboardWidgetService service = new HealthDashboardWidgetService(widgetMapper,
            dashboardMapper, petService);

    private HealthDashboardWidgetSummary summary(HealthDashboardItem item, int sortSeq, boolean savedYn) {
        HealthDashboardWidgetSummary s = new HealthDashboardWidgetSummary();
        s.setDshbdWdgtCd(item.getCode());
        s.setDshbdWdgtCdNm(item.name());
        s.setIndctYn(true);
        s.setSortSeq(sortSeq);
        s.setSavedYn(savedYn);
        return s;
    }

    private List<HealthDashboardWidgetSummary> allSummaries(boolean savedYn) {
        List<HealthDashboardWidgetSummary> list = new ArrayList<>();
        int seq = 10;
        for (HealthDashboardItem item : HealthDashboardItem.values()) {
            list.add(summary(item, seq, savedYn));
            seq += 10;
        }
        return list;
    }

    private List<Widget> allWidgets() {
        return Arrays.stream(HealthDashboardItem.values()).map(item -> new Widget(item.getCode(), true)).toList();
    }

    private PetSummary pet(Long petSn, String algyCn, String dssCn) {
        PetSummary p = new PetSummary();
        p.setPetSn(petSn);
        p.setAlgyCn(algyCn);
        p.setDssCn(dssCn);
        return p;
    }

    private HealthRecordedItemSummary recorded(Long petSn) {
        HealthRecordedItemSummary r = new HealthRecordedItemSummary();
        r.setPetSn(petSn);
        return r;
    }

    @Test
    void 저장한_적이_없으면_savedYn이_false이고_13개를_기본_순서로_준다() {
        when(widgetMapper.selectWidgetList(eq(MBR), eq("HLTG25"))).thenReturn(allSummaries(false));
        when(petService.searchMyPets(MBR)).thenReturn(List.of());

        HealthDashboardWidgetResponse res = service.read(MBR, TODAY);

        assertThat(res.savedYn()).isFalse();
        assertThat(res.widgets()).hasSize(13);
        assertThat(res.widgets().get(0).getDshbdWdgtCd()).isEqualTo(HealthDashboardItem.WEIGHT.getCode());
    }

    @Test
    void 저장한_행이_있으면_savedYn이_true() {
        when(widgetMapper.selectWidgetList(eq(MBR), eq("HLTG25"))).thenReturn(allSummaries(true));
        when(petService.searchMyPets(MBR)).thenReturn(List.of());

        assertThat(service.read(MBR, TODAY).savedYn()).isTrue();
    }

    @Test
    void 저장은_보낸_순서대로_10씩_순서를_주고_표시여부를_함께_저장한다() {
        when(widgetMapper.selectWidgetList(eq(MBR), anyString())).thenReturn(allSummaries(true));
        when(petService.searchMyPets(MBR)).thenReturn(List.of());
        List<Widget> widgets = new ArrayList<>(allWidgets());
        Collections.reverse(widgets);
        widgets.set(0, new Widget(HealthDashboardItem.CARE.getCode(), false));

        service.save(MBR, new HealthDashboardWidgetSaveRequest(widgets));

        verify(widgetMapper).upsertWidget(MBR, HealthDashboardItem.CARE.getCode(), "N", 10);
        verify(widgetMapper).upsertWidget(MBR, HealthDashboardItem.VACC.getCode(), "Y", 20);
        verify(widgetMapper).upsertWidget(MBR, HealthDashboardItem.WEIGHT.getCode(), "Y", 130);
    }

    @Test
    void 모르는_코드는_400이고_아무것도_저장하지_않는다() {
        List<Widget> widgets = new ArrayList<>(allWidgets());
        widgets.set(3, new Widget("HLTC9999", true));

        assertThatThrownBy(() -> service.save(MBR, new HealthDashboardWidgetSaveRequest(widgets)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        verify(widgetMapper, never()).upsertWidget(anyLong(), anyString(), anyString(), anyInt());
    }

    @Test
    void 중복_코드는_400이고_아무것도_저장하지_않는다() {
        List<Widget> widgets = new ArrayList<>(allWidgets());
        widgets.set(3, new Widget(HealthDashboardItem.WEIGHT.getCode(), true));

        assertThatThrownBy(() -> service.save(MBR, new HealthDashboardWidgetSaveRequest(widgets)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        verify(widgetMapper, never()).upsertWidget(anyLong(), anyString(), anyString(), anyInt());
    }

    @Test
    void 빠진_코드는_400이고_아무것도_저장하지_않는다() {
        List<Widget> widgets = new ArrayList<>(allWidgets());
        widgets.remove(12);

        assertThatThrownBy(() -> service.save(MBR, new HealthDashboardWidgetSaveRequest(widgets)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
        verify(widgetMapper, never()).upsertWidget(anyLong(), anyString(), anyString(), anyInt());
    }

    @Test
    void 반려동물이_없으면_기록_조회_쿼리를_부르지_않고_빈_목록() {
        when(widgetMapper.selectWidgetList(eq(MBR), anyString())).thenReturn(allSummaries(false));
        when(petService.searchMyPets(MBR)).thenReturn(List.of());

        assertThat(service.read(MBR, TODAY).recordedItems()).isEmpty();
        verify(dashboardMapper, never()).selectRecordedItems(any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any());
    }

    @Test
    void 기록_있는_항목은_반려동물별로_기본_순서의_코드로_매핑하고_알레르기는_PET_컬럼으로_판정한다() {
        when(widgetMapper.selectWidgetList(eq(MBR), anyString())).thenReturn(allSummaries(true));
        when(petService.searchMyPets(MBR)).thenReturn(List.of(pet(10L, "닭고기", null), pet(11L, " ", "")));
        HealthRecordedItemSummary first = recorded(10L);
        first.setWeight(true);
        first.setCkup(true);
        first.setVacc(true);
        HealthRecordedItemSummary second = recorded(11L);
        second.setPoop(true);
        second.setCare(true);
        when(dashboardMapper.selectRecordedItems(eq(List.of(10L, 11L)), eq(TODAY), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString())).thenReturn(List.of(second, first));

        List<HealthDashboardWidgetResponse.PetRecordedItems> items = service.read(MBR, TODAY).recordedItems();

        assertThat(items).hasSize(2);
        assertThat(items.get(0).petSn()).isEqualTo(10L);
        assertThat(items.get(0).dshbdWdgtCds()).containsExactly(HealthDashboardItem.WEIGHT.getCode(),
                HealthDashboardItem.CKUP.getCode(), HealthDashboardItem.ALLERGY.getCode(),
                HealthDashboardItem.VACC.getCode());
        assertThat(items.get(1).petSn()).isEqualTo(11L);
        assertThat(items.get(1).dshbdWdgtCds()).containsExactly(HealthDashboardItem.POOP.getCode(),
                HealthDashboardItem.CARE.getCode());
    }
}
