package com.nyangmeong.community.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.community.domain.CommunityAiCommentary;

/**
 * [커뮤니티 AI 코멘트 Mapper]
 */
@Mapper
public interface CommunityAiCommentaryMapper {

    /**
     * 일자별 사용 중인 코멘트 조회
     */
    CommunityAiCommentary selectByYmd(LocalDate cmtryCrtrYmd);

    /**
     * 관리자 코멘트 목록 (사용 안 함 포함, 최근 일자순)
     */
    List<CommunityAiCommentary> selectAdminList();

    /**
     * 번호로 코멘트 조회 (사용 여부 무관)
     */
    CommunityAiCommentary selectById(Long cmntyAiCmtrySn);

    /**
     * 사용 여부 변경
     */
    int updateUseYn(@Param("cmntyAiCmtrySn") Long cmntyAiCmtrySn, @Param("useYn") String useYn);

    /**
     * 일자별 코멘트 등록 : 그 일자 행이 이미 있으면 넣지 않고 0
     */
    int insertIgnore(CommunityAiCommentary commentary);
}
