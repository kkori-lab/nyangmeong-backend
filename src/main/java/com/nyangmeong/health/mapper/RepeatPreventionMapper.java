package com.nyangmeong.health.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.health.domain.RepeatPrevention;
import com.nyangmeong.health.dto.RepeatPreventionSearchCond;
import com.nyangmeong.health.dto.RepeatPreventionSummary;

/**
 * [반복 예방 관리 매퍼]
 * - REPT_PRVNT_MNG
 */
@Mapper
public interface RepeatPreventionMapper {

    /**
     * 반복 예방 목록 조회
     */
    List<RepeatPreventionSummary> selectList(@Param("cond") RepeatPreventionSearchCond cond);

    /**
     * 검진기록들의 반복 예방 목록 조회
     */
    List<RepeatPreventionSummary> selectListByCheckups(@Param("ckupRcdSns") List<Long> ckupRcdSns);

    /**
     * 반복 예방 조회
     */
    RepeatPrevention selectPrevention(Long reptPrvntMngSn);

    /**
     * 반복 예방 등록
     */
    int insertPrevention(RepeatPrevention prevention);

    /**
     * 반복 예방 해지
     */
    int deletePrevention(@Param("reptPrvntMngSn") Long reptPrvntMngSn, @Param("actorId") String actorId);

    /**
     * 공통코드명 조회
     */
    String selectCodeName(String comCd);

    /**
     * 반려동물의 반복 예방 관리 전체 삭제 (반려동물 삭제)
     */
    int deleteByPetSn(@Param("petSn") Long petSn, @Param("actorId") String actorId);
}
