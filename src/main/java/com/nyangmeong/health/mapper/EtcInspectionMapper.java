package com.nyangmeong.health.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.health.domain.EtcInspection;
import com.nyangmeong.health.dto.EtcInspectionSearchCond;
import com.nyangmeong.health.dto.EtcInspectionSummary;

/**
 * [기타검사 매퍼]
 * - ETC_INSP
 */
@Mapper
public interface EtcInspectionMapper {

    /**
     * 기타검사 목록 조회
     */
    List<EtcInspectionSummary> selectList(@Param("cond") EtcInspectionSearchCond cond);

    /**
     * 기타검사 조회
     */
    EtcInspection selectInspection(Long etcInspSn);

    /**
     * 기타검사 등록
     */
    int insertInspection(EtcInspection inspection);

    /**
     * 기타검사 수정
     */
    int updateInspection(@Param("inspection") EtcInspection inspection, @Param("actorId") String actorId);

    /**
     * 기타검사 삭제
     */
    int deleteInspection(@Param("etcInspSn") Long etcInspSn, @Param("actorId") String actorId);

    /**
     * 반려동물의 기타검사 전체 삭제 (반려동물 삭제)
     */
    int deleteByPetSn(@Param("petSn") Long petSn, @Param("actorId") String actorId);
}
