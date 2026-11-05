package com.nyangmeong.expense.mapper;

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

import com.nyangmeong.expense.dto.AdminReceiptSearchCond;

/**
 * [검사 - 관리자 영수증 OCR 현황 SQL]
 * - DB 없이 Mapper XML 에서 만들어지는 SQL 의 조건·정렬, OCR 응답 미조회만 확인
 */
class AdminReceiptMapperSqlTest {

    private static Configuration configuration;

    @BeforeAll
    static void loadMapper() throws Exception {
        configuration = new Configuration();
        String path = "mapper/expense/ReceiptMapper.xml";
        try (InputStream in = AdminReceiptMapperSqlTest.class.getResourceAsStream("/" + path)) {
            new XMLMapperBuilder(in, configuration, path, configuration.getSqlFragments()).parse();
        }
    }

    private String sql(String statement, AdminReceiptSearchCond cond) {
        Map<String, Object> param = new HashMap<>();
        param.put("cond", cond);
        param.put("scsCds", List.of("AIMC0002", "AIMC0003", "AIMC0007"));
        param.put("failCd", "AIMC0004");
        param.put("prcsCd", "AIMC0001");
        param.put("errCd", "AIMC0005");
        return configuration.getMappedStatement("com.nyangmeong.expense.mapper.ReceiptMapper." + statement)
                .getBoundSql(param).getSql().replaceAll("\\s+", " ").strip();
    }

    private AdminReceiptSearchCond full() {
        AdminReceiptSearchCond cond = new AdminReceiptSearchCond();
        cond.setFromYmd(LocalDate.of(2026, 11, 1));
        cond.setToYmd(LocalDate.of(2026, 11, 5));
        cond.setOcrDmndSrcCd("AIMC0027");
        cond.setOcrPrcsSttsCd("AIMC0004");
        return cond;
    }

    @Test
    void 건수_묶음은_기간과_출처만_걸고_상태_필터는_무시한다() {
        String sql = sql("selectAdminReceiptSummary", full());

        assertThat(sql).contains("R.REG_DT >= ?").contains("R.REG_DT < DATE_ADD(?, INTERVAL 1 DAY)")
                .contains("R.OCR_DMND_SRC_CD = ?").doesNotContain("AND R.OCR_PRCS_STTS_CD = ?")
                .contains("IN ( ? , ? , ? )");
    }

    @Test
    void 목록은_상태까지_걸고_최신순이며_OCR_응답을_읽지_않는다() {
        String sql = sql("selectAdminReceiptList", full());

        assertThat(sql).contains("JOIN MBR M ON M.MBR_SN = R.MBR_SN").contains("R.OCR_DMND_SRC_CD = ?")
                .contains("AND R.OCR_PRCS_STTS_CD = ?").doesNotContain("OCR_RSPNS_JSON")
                .endsWith("ORDER BY R.REG_DT DESC , R.RECPT_SN DESC");
    }

    @Test
    void 조건이_없으면_WHERE가_없다() {
        assertThat(sql("selectAdminReceiptList", new AdminReceiptSearchCond())).doesNotContain("WHERE");
        assertThat(sql("selectAdminReceiptSummary", new AdminReceiptSearchCond())).doesNotContain("WHERE");
    }
}
