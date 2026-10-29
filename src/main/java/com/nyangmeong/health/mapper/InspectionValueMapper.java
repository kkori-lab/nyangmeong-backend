package com.nyangmeong.health.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.health.domain.InspectionValue;
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

    /**
     * 검사 수치 조회
     */
    InspectionValue selectValue(Long inspNvlSn);

    /**
     * 검진기록의 같은 항목 검사 수치 조회
     */
    InspectionValue selectValueByArticle(@Param("ckupRcdSn") Long ckupRcdSn,
            @Param("inspArtclCd") String inspArtclCd, @Param("inspArtclDrctInptNm") String inspArtclDrctInptNm);

    /**
     * 검진기록의 정렬순서 최댓값 조회 (없으면 0)
     */
    int selectMaxSortSeq(Long ckupRcdSn);

    /**
     * 검사 수치 등록
     */
    int insertValue(InspectionValue value);

    /**
     * 삭제된 같은 항목 검사 수치 복구 (새 값으로 덮어씀)
     */
    int restoreValue(@Param("value") InspectionValue value, @Param("actorId") String actorId);

    /**
     * 검사 수치 수정
     */
    int updateValue(@Param("value") InspectionValue value, @Param("actorId") String actorId);
}
