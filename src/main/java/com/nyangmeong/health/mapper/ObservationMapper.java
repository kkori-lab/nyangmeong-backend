package com.nyangmeong.health.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.health.domain.ObservationCycle;
import com.nyangmeong.health.domain.ObservationItem;
import com.nyangmeong.health.dto.ObservationCycleSearchCond;
import com.nyangmeong.health.dto.ObservationCycleSummary;
import com.nyangmeong.health.dto.ObservationItemSearchCond;
import com.nyangmeong.health.dto.ObservationItemSummary;

/**
 * [관찰 매퍼]
 * - OBSRV_ARTCL, OBSRV_ARTCL_STTS_HSTRY, OBSRV_CYCL
 */
@Mapper
public interface ObservationMapper {

    /**
     * 관찰 항목 목록 조회(회차 요약은 서비스가 채움)
     */
    List<ObservationItemSummary> selectItems(@Param("cond") ObservationItemSearchCond cond);

    /**
     * 반려동물의 모든 항목 회차 조회(항목, 관찰일 순)
     */
    List<ObservationCycleSummary> selectPetCycles(@Param("petSn") Long petSn);

    /**
     * 관찰 회차 목록 조회(관찰일 오름차순, obsrvCyclSn을 주면 그 1건)
     */
    List<ObservationCycleSummary> selectCycles(@Param("cond") ObservationCycleSearchCond cond);

    /**
     * 관찰 항목 조회(경로 반려동물 기준)
     */
    ObservationItem selectItem(@Param("petSn") Long petSn, @Param("obsrvArtclSn") Long obsrvArtclSn);

    /**
     * 관찰 회차 조회(경로 반려동물 기준)
     */
    ObservationCycle selectCycle(@Param("petSn") Long petSn, @Param("obsrvCyclSn") Long obsrvCyclSn);

    /**
     * 항목의 남은 회차 수
     */
    int countCycles(@Param("obsrvArtclSn") Long obsrvArtclSn);

    /**
     * 관찰 중 항목이 있는 반려동물 번호 조회
     */
    List<Long> selectObservingPetSns(@Param("obsrvSttsCd") String obsrvSttsCd);

    /**
     * 공통코드 그룹의 하위 코드 존재 여부
     */
    boolean existsChildCode(@Param("prntComCd") String prntComCd, @Param("comCd") String comCd);

    /**
     * 관찰 항목 등록
     */
    int insertItem(ObservationItem item);

    /**
     * 관찰 항목 상태 이력 등록
     */
    int insertItemStatusHistory(@Param("obsrvArtclSn") Long obsrvArtclSn, @Param("mbrSn") Long mbrSn,
            @Param("bfrSttsCd") String bfrSttsCd, @Param("aftrSttsCd") String aftrSttsCd);

    /**
     * 관찰 종료(상태·종료일 변경)
     */
    int updateItemEnd(@Param("obsrvArtclSn") Long obsrvArtclSn, @Param("obsrvSttsCd") String obsrvSttsCd,
            @Param("actorId") String actorId);

    /**
     * 관찰 항목 삭제
     */
    int deleteItem(@Param("obsrvArtclSn") Long obsrvArtclSn, @Param("actorId") String actorId);

    /**
     * 관찰 회차 등록
     */
    int insertCycle(ObservationCycle cycle);

    /**
     * 관찰 회차 수정
     */
    int updateCycle(@Param("cycle") ObservationCycle cycle, @Param("actorId") String actorId);

    /**
     * 관찰 회차 삭제
     */
    int deleteCycle(@Param("obsrvCyclSn") Long obsrvCyclSn, @Param("actorId") String actorId);

    /**
     * 반려동물의 관찰 회차 전체 삭제 (반려동물 삭제)
     */
    int deleteCyclesByPetSn(@Param("petSn") Long petSn, @Param("actorId") String actorId);

    /**
     * 반려동물의 관찰 항목 전체 삭제 (반려동물 삭제)
     */
    int deleteByPetSn(@Param("petSn") Long petSn, @Param("actorId") String actorId);
}
