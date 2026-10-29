package com.nyangmeong.health.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.nyangmeong.health.domain.InspectionArticle;

/**
 * [검사결과지 PDF 분석]
 * - 외부 API 결정 전 가짜 결과 (issues 6) : PDF 내용과 무관한 고정 인식 결과
 * - 연결 시 이 클래스만 교체
 */
@Component
public class InspectionAnalyzer {

    /**
     * 인식한 검사 항목
     */
    public record Recognized(String inspArtclCd, String inspArtclDrctInptNm, BigDecimal rsltNvl, String unitNm,
            BigDecimal refLlmtNvl, BigDecimal refUplmtNvl) {
    }

    /**
     * 분석 결과
     * - excludedArtclNms : 혈액검사가 아니라서 뺀 항목 이름
     */
    public record Analysis(List<Recognized> items, List<String> excludedArtclNms) {
    }

    /**
     * 검사결과지 분석
     */
    public Analysis analyze(byte[] pdf) {
        return new Analysis(List.of(
                new Recognized(InspectionArticle.WBC.getCode(), null, new BigDecimal("8.2"), "K/μL",
                        new BigDecimal("5.5"), new BigDecimal("19.5")),
                new Recognized(InspectionArticle.CREA.getCode(), null, new BigDecimal("1.8"), "mg/dL",
                        new BigDecimal("0.8"), new BigDecimal("1.5"))),
                List.of("체온"));
    }
}
