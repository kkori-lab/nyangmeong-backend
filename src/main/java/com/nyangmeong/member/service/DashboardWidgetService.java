package com.nyangmeong.member.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.member.dto.DashboardWidgetSaveRequest;
import com.nyangmeong.member.dto.DashboardWidgetSummary;
import com.nyangmeong.member.mapper.DashboardWidgetMapper;

import lombok.RequiredArgsConstructor;

/**
 * [메인 위젯 순서 서비스]
 * - FN-MAN-006·007, 위젯은 전부 보이고 회원 단위로 순서만 저장한다(탭을 바꿔도 같은 순서)
 */
@Service
@RequiredArgsConstructor
public class DashboardWidgetService {

    /** 대시보드 위젯 공통코드(MANG01) */
    static final String WIDGET_GROUP_CD = "MANG01";
    private static final int SORT_STEP = 10;

    private final DashboardWidgetMapper dashboardWidgetMapper;

    /**
     * 위젯 순서 조회 : 저장한 적 없으면 기본 순서
     */
    @Transactional(readOnly = true)
    public List<DashboardWidgetSummary> getWidgets(Long mbrSn) {
        return dashboardWidgetMapper.selectWidgetList(mbrSn, WIDGET_GROUP_CD);
    }

    /**
     * 위젯 순서 저장 : 사용 중인 코드를 전부 한 번씩 담지 않으면 INVALID_INPUT_VALUE
     */
    @Transactional
    public List<DashboardWidgetSummary> saveWidgets(Long mbrSn, DashboardWidgetSaveRequest req) {
        List<String> cds = req.dshbdWdgtCds();
        Set<String> allowed = new HashSet<>();
        for (DashboardWidgetSummary w : dashboardWidgetMapper.selectWidgetList(mbrSn, WIDGET_GROUP_CD)) {
            allowed.add(w.getDshbdWdgtCd());
        }
        if (cds.size() != allowed.size() || !allowed.equals(new HashSet<>(cds))) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        for (int i = 0; i < cds.size(); i++) {
            dashboardWidgetMapper.upsertWidgetSort(mbrSn, cds.get(i), (i + 1) * SORT_STEP);
        }
        return dashboardWidgetMapper.selectWidgetList(mbrSn, WIDGET_GROUP_CD);
    }
}
