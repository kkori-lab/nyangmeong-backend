package com.nyangmeong.community.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.community.dto.AdminCareMatchResponse;

/**
 * [펫돌봄 매칭 Mapper]
 * - 지금은 관리자 상세 조회만 (SCR-ADM-022). 채팅 메시지는 읽지 않는다
 */
@Mapper
public interface CareMatchMapper {

    /**
     * 모집글의 매칭 목록 (최신순)
     */
    List<AdminCareMatchResponse.Match> selectAdminMatches(@Param("petPrtcRcrtSn") Long petPrtcRcrtSn);

    /**
     * 모집글 매칭들의 합의 조건 버전 (매칭별 버전 내림차순)
     */
    List<AdminCareMatchResponse.ConditionVersion> selectAdminConditionVersions(
            @Param("petPrtcRcrtSn") Long petPrtcRcrtSn);

    /**
     * 모집글 매칭들의 상태 이력 (오래된 순)
     */
    List<AdminCareMatchResponse.StatusHistory> selectAdminStatusHistories(
            @Param("petPrtcRcrtSn") Long petPrtcRcrtSn);

    /**
     * 모집글 매칭들의 돌봄 완료 요청 회차 (회차 순)
     */
    List<AdminCareMatchResponse.CompletionRequest> selectAdminCompletionRequests(
            @Param("petPrtcRcrtSn") Long petPrtcRcrtSn);

    /**
     * 모집글 매칭들의 돌봄 완료 사진 (정렬 순서)
     */
    List<AdminCareMatchResponse.Photo> selectAdminCompletionPhotos(@Param("petPrtcRcrtSn") Long petPrtcRcrtSn);
}
