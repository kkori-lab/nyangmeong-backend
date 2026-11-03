package com.nyangmeong.global.external;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.response.PageQuery;
import com.nyangmeong.global.response.PageResponse;

import lombok.RequiredArgsConstructor;

/**
 * [외부 API 호출 로그 기록·사용량 조회]
 * - SCR-ADM-112 (FN-ADM-112-01·02·03)
 * - 기록은 Python 통지만 받음. Java 호출부는 실제 구현을 붙일 때 기록
 */
@Service
@RequiredArgsConstructor
public class ExternalApiUsageService {

    private static final int DEFAULT_DAYS = 7;
    private static final int MAX_DAYS = 92;

    private final ExternalApiCallLogMapper externalApiCallLogMapper;

    /**
     * 호출 통지 기록
     */
    @Transactional
    public ExternalApiCallResponse record(Long mbrSn, ExternalApiCallRequest req) {
        ExternalApiType.from(req.extlApiSeCd());
        ExternalApiCallResult.from(req.callRsltCd());
        ExternalApiCallLog log = ExternalApiCallLog.of(mbrSn, req);
        externalApiCallLogMapper.insert(log);
        return new ExternalApiCallResponse(log.getExtlApiCallLogSn());
    }

    /**
     * 종류별 사용량 조회, 호출이 없는 종류도 0으로 포함
     */
    @Transactional(readOnly = true)
    public List<ExternalApiUsageSummary> search(ExternalApiUsageSearchCond cond) {
        normalize(cond);
        Map<String, ExternalApiUsageSummary> usage = externalApiCallLogMapper
                .selectUsage(cond, ExternalApiCallResult.SUCCESS.getCode()).stream()
                .collect(Collectors.toMap(ExternalApiUsageSummary::getExtlApiSeCd, Function.identity()));
        return Arrays.stream(ExternalApiType.values())
                .filter(type -> cond.getExtlApiSeCd() == null || type.getCode().equals(cond.getExtlApiSeCd()))
                .map(type -> usage.getOrDefault(type.getCode(), ExternalApiUsageSummary.empty(type)))
                .toList();
    }

    /**
     * 일별 사용량 조회
     */
    @Transactional(readOnly = true)
    public List<ExternalApiDailyUsage> searchDaily(ExternalApiUsageSearchCond cond) {
        normalize(cond);
        return externalApiCallLogMapper.selectDailyUsage(cond, ExternalApiCallResult.SUCCESS.getCode());
    }

    /**
     * AI 일별 토큰 조회
     */
    @Transactional(readOnly = true)
    public List<ExternalApiDailyToken> searchTokens(ExternalApiUsageSearchCond cond) {
        normalize(cond);
        return externalApiCallLogMapper.selectDailyTokens(cond, ExternalApiType.aiCodes());
    }

    /**
     * 실패 목록 조회
     */
    @Transactional(readOnly = true)
    public PageResponse<ExternalApiFailureSummary> searchFailures(ExternalApiUsageSearchCond cond, PageQuery pq) {
        normalize(cond);
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOrDefault());
        List<ExternalApiFailureSummary> rows = externalApiCallLogMapper.selectFailures(cond,
                ExternalApiCallResult.SUCCESS.getCode());
        return PageResponse.of(new PageInfo<>(rows));
    }

    // 기간 기본값을 채우고 종류·기간을 검사
    private void normalize(ExternalApiUsageSearchCond cond) {
        if (cond.getExtlApiSeCd() != null) {
            ExternalApiType.from(cond.getExtlApiSeCd());
        }
        LocalDate endYmd = cond.getEndYmd() != null ? cond.getEndYmd() : LocalDate.now();
        LocalDate bgngYmd = cond.getBgngYmd() != null ? cond.getBgngYmd() : endYmd.minusDays(DEFAULT_DAYS - 1);
        if (bgngYmd.isAfter(endYmd) || ChronoUnit.DAYS.between(bgngYmd, endYmd) + 1 > MAX_DAYS) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        cond.setBgngYmd(bgngYmd);
        cond.setEndYmd(endYmd);
    }
}
