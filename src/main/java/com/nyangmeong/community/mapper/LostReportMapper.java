package com.nyangmeong.community.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.community.domain.LostReport;
import com.nyangmeong.community.domain.LostReportDetail;
import com.nyangmeong.community.domain.LostReportStatusHistory;
import com.nyangmeong.community.dto.LostReportSummary;

/**
 * [실종 제보 Mapper]
 */
@Mapper
public interface LostReportMapper {

    /**
     * 목록 조회 (정상 게시글, 실종은 만료 전만, 실종 먼저·찾았어요 뒤)
     */
    List<LostReportSummary> selectList(@Param("mbrSn") Long mbrSn, @Param("lostTpofSttsCd") String lostTpofSttsCd,
            @Param("rgnSn") Long rgnSn, @Param("petSeCd") String petSeCd, @Param("wtnsPrd") String wtnsPrd,
            @Param("keyword") String keyword, @Param("pstSttsCd") String pstSttsCd,
            @Param("missingCd") String missingCd, @Param("foundCd") String foundCd);

    /**
     * 상세 조회 (게시글 상태 조건)
     */
    LostReportDetail selectDetail(@Param("lostTpofSn") Long lostTpofSn, @Param("pstSttsCd") String pstSttsCd);

    /**
     * 게시글로 제보 조회 (실종 게시글이 아니면 null)
     */
    LostReport selectByPstSn(Long pstSn);

    /**
     * 제보 등록 (만료 기준 일시는 지금부터 30일)
     */
    int insertLostReport(LostReport report);

    /**
     * 제보 상태 이력 등록
     */
    int insertStatusHistory(LostReportStatusHistory history);

    /**
     * 실종 상태 제보 수정
     */
    int updateLostReport(@Param("report") LostReport report, @Param("missingCd") String missingCd);

    /**
     * 찾았어요 전환 (실종 상태만)
     */
    int updateFound(@Param("lostTpofSn") Long lostTpofSn, @Param("missingCd") String missingCd,
            @Param("foundCd") String foundCd);

    /**
     * 게시 기간 30일 연장 (실종 상태, 종료 7일 전부터 종료 전까지만)
     */
    int updateRenewed(@Param("lostTpofSn") Long lostTpofSn, @Param("missingCd") String missingCd);
}
