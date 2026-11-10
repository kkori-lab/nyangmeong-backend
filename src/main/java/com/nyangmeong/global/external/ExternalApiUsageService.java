package com.nyangmeong.global.external;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
 * - 기록은 Python 통지와 Java 호출부(카카오 로컬, SMTP 메일)
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
     * Java 호출부 기록 : 새 트랜잭션
     * - 부른 쪽 트랜잭션이 롤백돼도 호출 로그는 남는다 (인증 메일 발송 실패 등)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExternalApiCallResponse recordInNewTransaction(ExternalApiCallRequest req) {
        return record(null, req);
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

    /**
     * 검사결과지 분석 현황 조회 (FN-ADM-082-03)
     * - PDF 분석(SYSC0025)만, 실패는 실패·시간초과. 기간은 모두 선택
     */
    @Transactional(readOnly = true)
    public PdfAnalysisListResponse searchPdfAnalyses(PdfAnalysisSearchCond cond, PageQuery pq) {
        if (cond.getFromYmd() != null && cond.getToYmd() != null && cond.getFromYmd().isAfter(cond.getToYmd())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String pdfCd = ExternalApiType.PDF_ANALYSIS.getCode();
        PdfAnalysisSummary summary = externalApiCallLogMapper.selectPdfAnalysisSummary(cond, pdfCd,
                ExternalApiCallResult.SUCCESS.getCode(), ExternalApiCallResult.TIMEOUT.getCode(),
                ExternalApiCallResult.FAIL.getCode());
        PageHelper.startPage(pq.pageOrDefault(), pq.sizeOrDefault());
        List<PdfAnalysisFailureRow> rows = externalApiCallLogMapper.selectPdfAnalysisFailures(cond, pdfCd,
                ExternalApiCallResult.SUCCESS.getCode());
        return PdfAnalysisListResponse.of(summary, PageResponse.of(new PageInfo<>(rows)));
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
