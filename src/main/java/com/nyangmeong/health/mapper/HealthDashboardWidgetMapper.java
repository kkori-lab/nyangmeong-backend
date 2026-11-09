package com.nyangmeong.health.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.health.dto.HealthDashboardWidgetSummary;

/**
 * [건강기록 대시보드 항목 매퍼]
 * - MBR_DSHBD_WDGT
 */
@Mapper
public interface HealthDashboardWidgetMapper {

    /**
     * 대시보드 항목 전부를 회원이 저장한 순서로, 저장한 적 없으면 공통코드 기본 순서로 조회
     */
    List<HealthDashboardWidgetSummary> selectWidgetList(@Param("mbrSn") Long mbrSn,
            @Param("prntComCd") String prntComCd);

    /**
     * 항목 하나의 표시여부·순서 저장(이미 있으면 바꿈)
     */
    int upsertWidget(@Param("mbrSn") Long mbrSn, @Param("dshbdWdgtCd") String dshbdWdgtCd,
            @Param("indctYn") String indctYn, @Param("sortSeq") int sortSeq);
}
