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
}
