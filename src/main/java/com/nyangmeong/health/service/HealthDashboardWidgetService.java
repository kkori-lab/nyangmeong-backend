package com.nyangmeong.health.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.health.domain.HealthCodeGroup;
import com.nyangmeong.health.domain.HealthDashboardItem;
import com.nyangmeong.health.dto.HealthDashboardWidgetResponse;
import com.nyangmeong.health.dto.HealthDashboardWidgetSaveRequest;
import com.nyangmeong.health.dto.HealthDashboardWidgetSaveRequest.Widget;
import com.nyangmeong.health.dto.HealthDashboardWidgetSummary;
import com.nyangmeong.health.mapper.HealthDashboardWidgetMapper;

import lombok.RequiredArgsConstructor;

/**
 * [건강기록 대시보드 고른 항목 서비스]
 * - 회원이 고른 대시보드 항목·순서 조회·저장 : 회원 단위(반려동물마다 따로 두지 않음)
 */
@Service
@RequiredArgsConstructor
public class HealthDashboardWidgetService {

    private static final int SORT_STEP = 10;

    private final HealthDashboardWidgetMapper healthDashboardWidgetMapper;

    /**
     * 고른 항목 조회
     */
    @Transactional(readOnly = true)
    public HealthDashboardWidgetResponse get(Long mbrSn) {
        return read(mbrSn);
    }

    /**
     * 고른 항목 전체 저장
     */
    @Transactional
    public HealthDashboardWidgetResponse save(Long mbrSn, HealthDashboardWidgetSaveRequest req) {
        checkAllItems(req.widgets());
        List<Widget> widgets = req.widgets();
        for (int i = 0; i < widgets.size(); i++) {
            Widget widget = widgets.get(i);
            healthDashboardWidgetMapper.upsertWidget(mbrSn, widget.dshbdWdgtCd(),
                    Boolean.TRUE.equals(widget.indctYn()) ? "Y" : "N", (i + 1) * SORT_STEP);
        }
        return read(mbrSn);
    }

    private HealthDashboardWidgetResponse read(Long mbrSn) {
        List<HealthDashboardWidgetSummary> widgets = healthDashboardWidgetMapper.selectWidgetList(mbrSn,
                HealthCodeGroup.DASHBOARD_ITEM.getCode());
        boolean savedYn = widgets.stream().anyMatch(HealthDashboardWidgetSummary::isSavedYn);
        return new HealthDashboardWidgetResponse(savedYn, widgets);
    }

    /**
     * 13개 코드를 한 번씩 담았는지 검사(모르는·중복·빠진 코드)
     */
    private void checkAllItems(List<Widget> widgets) {
        Set<String> codes = new HashSet<>();
        for (Widget widget : widgets) {
            boolean known = HealthDashboardItem.of(widget.dshbdWdgtCd()).isPresent();
            if (!known || !codes.add(widget.dshbdWdgtCd())) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }
        if (codes.size() != HealthDashboardItem.values().length) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
