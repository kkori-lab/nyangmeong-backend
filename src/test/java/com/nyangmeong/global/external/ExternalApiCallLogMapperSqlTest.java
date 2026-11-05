package com.nyangmeong.global.external;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * [검사 - 외부 API 호출 로그 SQL]
 * - DB 없이 Mapper XML 에서 만들어지는 SQL 의 조건·집계·정렬만 확인
 */
class ExternalApiCallLogMapperSqlTest {

    private static final String NAMESPACE = "com.nyangmeong.global.external.ExternalApiCallLogMapper.";
    private static final String PERIOD = "CALL_DT >= ? AND CALL_DT < DATE_ADD(?, INTERVAL 1 DAY)";

    private static Configuration configuration;

    @BeforeAll
    static void loadMapper() throws Exception {
        configuration = new Configuration();
        try (InputStream in = ExternalApiCallLogMapperSqlTest.class
                .getResourceAsStream("/mapper/global/ExternalApiCallLogMapper.xml")) {
            new XMLMapperBuilder(in, configuration, "mapper/global/ExternalApiCallLogMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
    }

    private String sql(String id, String extlApiSeCd) {
        ExternalApiUsageSearchCond cond = new ExternalApiUsageSearchCond();
        cond.setBgngYmd(LocalDate.of(2026, 10, 30));
        cond.setEndYmd(LocalDate.of(2026, 10, 31));
        cond.setExtlApiSeCd(extlApiSeCd);
        Map<String, Object> param = new HashMap<>();
        param.put("cond", cond);
        param.put("successCd", "SYSC0021");
        param.put("aiCodes", List.of("SYSC0015", "SYSC0024"));
        return configuration.getMappedStatement(NAMESPACE + id).getBoundSql(param).getSql().replaceAll("\\s+", " ");
    }

    private String pdfSql(String id, LocalDate fromYmd, LocalDate toYmd) {
        PdfAnalysisSearchCond cond = new PdfAnalysisSearchCond();
        cond.setFromYmd(fromYmd);
        cond.setToYmd(toYmd);
        Map<String, Object> param = new HashMap<>();
        param.put("cond", cond);
        param.put("pdfCd", "SYSC0025");
        param.put("successCd", "SYSC0021");
        param.put("timeoutCd", "SYSC0023");
        param.put("failCd", "SYSC0022");
        return configuration.getMappedStatement(NAMESPACE + id).getBoundSql(param).getSql().replaceAll("\\s+", " ");
    }

    @Test
    void 검사결과지_분석_건수는_PDF_코드만_결과별로_세고_끝_날짜를_포함한다() {
        String sql = pdfSql("selectPdfAnalysisSummary", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5));

        assertThat(sql).contains("WHERE EXTL_API_SE_CD = ?");
        assertThat(sql).contains("COUNT(*) AS TOT_CNT");
        assertThat(sql).contains("COALESCE(SUM(CASE WHEN CALL_RSLT_CD = ? THEN 1 ELSE 0 END), 0) AS SCS_CNT");
        assertThat(sql).contains("AS TMOT_CNT");
        assertThat(sql).contains("AS ERR_CNT");
        assertThat(sql).contains("AND CALL_DT >= ? AND CALL_DT < DATE_ADD(?, INTERVAL 1 DAY)");
    }

    @Test
    void 검사결과지_분석_실패_목록은_성공을_빼고_최신순이며_기간이_없으면_걸지_않는다() {
        String sql = pdfSql("selectPdfAnalysisFailures", null, null);

        assertThat(sql).contains("WHERE EXTL_API_SE_CD = ? AND CALL_RSLT_CD <> ?");
        assertThat(sql).doesNotContain("CALL_DT >=");
        assertThat(sql).doesNotContain("DATE_ADD");
        assertThat(sql).contains("ORDER BY CALL_DT DESC, EXTL_API_CALL_LOG_SN DESC");
    }

    @Test
    void 종류별_사용량은_성공이_아닌_호출을_실패로_세고_끝_날짜를_포함한다() {
        String sql = sql("selectUsage", null);

        assertThat(sql).contains("COUNT(*) AS CALL_CNT");
        assertThat(sql).contains("SUM(CASE WHEN CALL_RSLT_CD = ? THEN 0 ELSE 1 END) AS FAIL_CNT");
        assertThat(sql).contains("ROUND(AVG(RSPNS_MS)) AS AVG_RSPNS_MS");
        assertThat(sql).contains(PERIOD);
        assertThat(sql).contains("GROUP BY EXTL_API_SE_CD");
        assertThat(sql).doesNotContain("EXTL_API_SE_CD = ?");
    }

    @Test
    void 일별_사용량은_날짜와_종류로_묶어_최신_날짜순이다() {
        String sql = sql("selectDailyUsage", "SYSC0016");

        assertThat(sql).contains("AND EXTL_API_SE_CD = ?");
        assertThat(sql).contains("GROUP BY DATE(CALL_DT), EXTL_API_SE_CD ORDER BY CALL_YMD DESC, EXTL_API_SE_CD");
    }

    @Test
    void 토큰은_AI_코드만_날짜별로_더한다() {
        String sql = sql("selectDailyTokens", null);

        assertThat(sql).containsPattern("WHERE EXTL_API_SE_CD IN \\( \\? , \\? \\)");
        assertThat(sql).contains("COALESCE(SUM(INPT_TKN_CNT), 0) AS INPT_TKN_CNT");
        assertThat(sql).contains("COALESCE(SUM(OTPT_TKN_CNT), 0) AS OTPT_TKN_CNT");
        assertThat(sql).contains("GROUP BY DATE(CALL_DT) ORDER BY CALL_YMD DESC");
    }

    @Test
    void 실패_목록은_성공_행을_빼고_최신순이다() {
        String sql = sql("selectFailures", null);

        assertThat(sql).contains("WHERE CALL_RSLT_CD <> ?");
        assertThat(sql).contains(PERIOD);
        assertThat(sql).contains("ORDER BY CALL_DT DESC, EXTL_API_CALL_LOG_SN DESC");
    }
}
