package com.nyangmeong.health.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.health.dto.HealthRecordedItemSummary;

/**
 * [건강기록 대시보드 기록 여부 매퍼]
 * - 항목별 기록 존재 여부(EXISTS)를 반려동물 1행으로 조회
 */
@Mapper
public interface HealthDashboardMapper {

    /**
     * 반려동물별 기록 있는 항목 조회(체중 제외 일 합계 3종은 기준일 이전 기록만)
     */
    List<HealthRecordedItemSummary> selectRecordedItems(@Param("petSns") List<Long> petSns,
            @Param("baseYmd") LocalDate baseYmd, @Param("weightCd") String weightCd,
            @Param("mealCd") String mealCd, @Param("waterCd") String waterCd, @Param("walkCd") String walkCd,
            @Param("stoolCd") String stoolCd, @Param("hospitalCd") String hospitalCd,
            @Param("completedCd") String completedCd, @Param("ongoingCd") String ongoingCd,
            @Param("vaccinationCd") String vaccinationCd, @Param("regularCd") String regularCd,
            @Param("dailyZoneCd") String dailyZoneCd);
}
