package com.nyangmeong.support.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.nyangmeong.support.domain.Faq;

/**
 * [검사 - 관리자 FAQ SQL]
 * - DB 없이 Mapper XML 에서 만들어지는 SQL 의 조건·정렬만 확인
 */
class AdminFaqMapperSqlTest {

    private static final String NS = "com.nyangmeong.support.mapper.FaqMapper.";

    private static Configuration configuration;

    @BeforeAll
    static void loadMapper() throws Exception {
        configuration = new Configuration();
        try (InputStream in = AdminFaqMapperSqlTest.class.getResourceAsStream("/mapper/support/FaqMapper.xml")) {
            new XMLMapperBuilder(in, configuration, "mapper/support/FaqMapper.xml", configuration.getSqlFragments())
                    .parse();
        }
    }

    private String sql(String id, Object param) {
        return configuration.getMappedStatement(NS + id).getBoundSql(param).getSql().replaceAll("\\s+", " ");
    }

    private String adminListSql(String faqClsfCd, String keyword) {
        Map<String, Object> param = new HashMap<>();
        param.put("faqClsfCd", faqClsfCd);
        param.put("keyword", keyword);
        return sql("selectAdminList", param);
    }

    @Test
    void 관리자_목록은_비노출도_포함하고_정렬_순서_다음_번호순이다() {
        String sql = adminListSql(null, null);

        assertThat(sql).doesNotContain("WHERE").doesNotContain("USE_YN = 'Y' ORDER");
        assertThat(sql).contains("ORDER BY F.SORT_SEQ ASC, F.FAQ_SN ASC");
    }

    @Test
    void 카테고리와_검색은_AND로_건다() {
        String sql = adminListSql("CSCC0002", "비밀번호");

        assertThat(sql).contains("WHERE F.FAQ_CLSF_CD = ? AND ( F.QSTN_TTL LIKE CONCAT('%', ?, '%') "
                + "OR F.ANS_CN LIKE CONCAT('%', ?, '%') )");
    }

    @Test
    void 검색만_있으면_AND_없이_시작한다() {
        String sql = adminListSql(null, "비밀번호");

        assertThat(sql).contains("WHERE ( F.QSTN_TTL LIKE").doesNotContain("WHERE AND");
    }

    @Test
    void 관리자_단건은_사용_여부를_보지_않는다() {
        assertThat(sql("selectAdminFaq", 5L)).doesNotContain("USE_YN =");
    }

    @Test
    void 사용자_목록은_사용_중인_것만_본다() {
        Map<String, Object> param = new HashMap<>();
        param.put("faqClsfCd", null);
        param.put("keyword", null);

        assertThat(sql("selectList", param)).contains("WHERE F.USE_YN = 'Y'");
    }

    @Test
    void 수정은_보낸_값만_바꾼다() {
        Faq faq = new Faq();
        faq.setFaqSn(5L);
        faq.setUseYn("N");
        faq.setMdfrId("9");

        String sql = sql("update", faq);

        assertThat(sql).contains("USE_YN = ?").contains("WHERE FAQ_SN = ?");
        assertThat(sql).doesNotContain("QSTN_TTL = ?").doesNotContain("ANS_CN = ?").doesNotContain("SORT_SEQ = ?")
                .doesNotContain("FAQ_CLSF_CD = ?");
    }

    @Test
    void 등록은_등록자와_수정자를_넣는다() {
        assertThat(sql("insert", new Faq())).contains("RGTR_ID").contains("MDFR_ID");
    }
}
