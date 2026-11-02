package com.nyangmeong.global.external;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * [외부 API 호출 로그 Mapper]
 * - 영구 보관 (FN-ADM-112-04) : 삭제 쿼리 없음
 */
@Mapper
public interface ExternalApiCallLogMapper {

    /**
     * 호출 로그 추가
     */
    int insert(ExternalApiCallLog log);

    /**
     * 종류별 사용량 조회
     */
    List<ExternalApiUsageSummary> selectUsage(@Param("cond") ExternalApiUsageSearchCond cond,
            @Param("successCd") String successCd);

    /**
     * 일별 사용량 조회
     */
    List<ExternalApiDailyUsage> selectDailyUsage(@Param("cond") ExternalApiUsageSearchCond cond,
            @Param("successCd") String successCd);

    /**
     * AI 일별 토큰 조회
     */
    List<ExternalApiDailyToken> selectDailyTokens(@Param("cond") ExternalApiUsageSearchCond cond,
            @Param("aiCodes") List<String> aiCodes);

    /**
     * 실패 목록 조회
     */
    List<ExternalApiFailureSummary> selectFailures(@Param("cond") ExternalApiUsageSearchCond cond,
            @Param("successCd") String successCd);
}
