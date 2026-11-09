package com.nyangmeong.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.nyangmeong.health.mapper.HealthDashboardWidgetMapper;

/**
 * [검사 - 건강기록 대시보드 고른 항목]
 * - 저장 유무 구분, 순서·표시여부 저장, 모르는·중복·빠진 코드 400
 */
class HealthDashboardWidgetServiceTest {

    private static final Long MBR = 1L;

    private final HealthDashboardWidgetMapper widgetMapper = mock(HealthDashboardWidgetMapper.class);
    private final HealthDashboardWidgetService service = new HealthDashboardWidgetService(widgetMapper);

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

    @Test
    void 저장한_적이_없으면_savedYn이_false이고_13개를_기본_순서로_준다() {
        when(widgetMapper.selectWidgetList(eq(MBR), eq("HLTG25"))).thenReturn(allSummaries(false));

        HealthDashboardWidgetResponse res = service.get(MBR);

        assertThat(res.savedYn()).isFalse();
        assertThat(res.widgets()).hasSize(13);
        assertThat(res.widgets().get(0).getDshbdWdgtCd()).isEqualTo(HealthDashboardItem.WEIGHT.getCode());
    }

    @Test
    void 저장한_행이_있으면_savedYn이_true() {
        when(widgetMapper.selectWidgetList(eq(MBR), eq("HLTG25"))).thenReturn(allSummaries(true));

        assertThat(service.get(MBR).savedYn()).isTrue();
    }

    @Test
    void 저장은_보낸_순서대로_10씩_순서를_주고_표시여부를_함께_저장한다() {
        when(widgetMapper.selectWidgetList(eq(MBR), anyString())).thenReturn(allSummaries(true));
        List<Widget> widgets = new ArrayList<>(allWidgets());
        Collections.reverse(widgets);
        widgets.set(0, new Widget(HealthDashboardItem.CARE.getCode(), false));

        HealthDashboardWidgetResponse res = service.save(MBR, new HealthDashboardWidgetSaveRequest(widgets));

        verify(widgetMapper).upsertWidget(MBR, HealthDashboardItem.CARE.getCode(), "N", 10);
        verify(widgetMapper).upsertWidget(MBR, HealthDashboardItem.VACC.getCode(), "Y", 20);
        verify(widgetMapper).upsertWidget(MBR, HealthDashboardItem.WEIGHT.getCode(), "Y", 130);
        assertThat(res.savedYn()).isTrue();
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
}
