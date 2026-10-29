package com.nyangmeong.health.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.health.dto.InspectionValueSearchCond;
import com.nyangmeong.health.dto.InspectionValueSummary;

/**
 * [검사 수치 매퍼]
 * - INSP_NVL
 */
@Mapper
public interface InspectionValueMapper {

    /**
     * 검사 수치 목록 조회
     */
    List<InspectionValueSummary> selectList(@Param("cond") InspectionValueSearchCond cond);

    /**
     * 검사 수치 목록 조회 (검사 항목만)
     */
    List<InspectionValueSummary> selectArticles(Long ckupRcdSn);

    /**
     * 검사 수치들의 검진기록 번호 조회
     */
    List<Long> selectCheckupSns(@Param("inspNvlSns") List<Long> inspNvlSns);

    /**
     * 검사 수치 삭제
     */
    int deleteValues(@Param("inspNvlSns") List<Long> inspNvlSns, @Param("actorId") String actorId);
}
