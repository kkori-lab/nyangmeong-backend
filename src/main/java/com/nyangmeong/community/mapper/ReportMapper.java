package com.nyangmeong.community.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.community.domain.Report;
import com.nyangmeong.community.domain.ReportStatusHistory;
import com.nyangmeong.community.dto.AdminReportAutoBlindSummary;
import com.nyangmeong.community.dto.AdminReportSearchCond;
import com.nyangmeong.community.dto.AdminReportStatusHistorySummary;
import com.nyangmeong.community.dto.AdminReportSummary;

/**
 * [신고 Mapper]
 */
@Mapper
public interface ReportMapper {

    /**
     * 같은 회원의 같은 대상 신고 여부 조회
     */
    boolean existsReport(@Param("mbrSn") Long mbrSn, @Param("pstSn") Long pstSn, @Param("cmntSn") Long cmntSn);

    /**
     * 대상별 신고 수 조회
     */
    int countReports(@Param("pstSn") Long pstSn, @Param("cmntSn") Long cmntSn);

    /**
     * 누적 판단용 대상별 신고 수 조회 (반려 제외)
     */
    int countAccumulatedReports(@Param("pstSn") Long pstSn, @Param("cmntSn") Long cmntSn,
            @Param("rejectedCd") String rejectedCd);

    /**
     * 신고 등록
     */
    int insertReport(Report report);

    /**
     * 신고 상태 이력 등록
     */
    int insertStatusHistory(ReportStatusHistory history);

    /**
     * 신고 단건 조회
     */
    Report selectReport(Long dclrSn);

    /**
     * 대상의 상태별 신고 조회
     */
    List<Report> selectReportsByStatus(@Param("pstSn") Long pstSn, @Param("cmntSn") Long cmntSn,
            @Param("status") String status);

    /**
     * 관리자 신고 목록 조회
     */
    List<AdminReportSummary> selectAdminList(@Param("cond") AdminReportSearchCond cond,
            @Param("postTarget") Boolean postTarget, @Param("blindCd") String blindCd,
            @Param("autoBlindRsn") String autoBlindRsn, @Param("autoDeleteRsn") String autoDeleteRsn);

    /**
     * 같은 대상 신고 목록 조회
     */
    List<AdminReportSummary> selectSameTargetList(@Param("pstSn") Long pstSn, @Param("cmntSn") Long cmntSn);

    /**
     * 자동 블라인드 대상 목록 조회
     */
    List<AdminReportAutoBlindSummary> selectAutoBlindList(@Param("receivedCd") String receivedCd,
            @Param("blindCd") String blindCd, @Param("autoBlindRsn") String autoBlindRsn,
            @Param("autoDeleteRsn") String autoDeleteRsn);

    /**
     * 신고 대상의 자동 블라인드 여부 조회
     */
    boolean existsAutoTarget(@Param("dclrSn") Long dclrSn, @Param("blindCd") String blindCd,
            @Param("autoBlindRsn") String autoBlindRsn, @Param("autoDeleteRsn") String autoDeleteRsn);

    /**
     * 신고 처리 이력 조회
     */
    List<AdminReportStatusHistorySummary> selectAdminStatusHistories(Long dclrSn);

    /**
     * 신고 처리 상태 변경
     */
    int updateProcessed(@Param("dclrSn") Long dclrSn, @Param("beforeCd") String beforeCd,
            @Param("afterCd") String afterCd, @Param("dclrActnCd") String dclrActnCd,
            @Param("prcsMbrSn") Long prcsMbrSn);

    /**
     * 처리 상태별 신고 수 조회
     */
    long countByStatus(@Param("dclrPrcsSttsCd") String dclrPrcsSttsCd);
}
