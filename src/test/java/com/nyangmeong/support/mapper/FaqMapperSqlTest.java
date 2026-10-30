package com.nyangmeong.support.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * [검사 - FAQ 목록 SQL]
 * - DB 없이 Mapper XML 에서 만들어지는 SQL 의 조건·정렬만 확인
 */
class FaqMapperSqlTest {

    private static Configuration configuration;

    @BeforeAll
    static void loadMapper() throws Exception {
        configuration = new Configuration();
        try (InputStream in = FaqMapperSqlTest.class.getResourceAsStream("/mapper/support/FaqMapper.xml")) {
            new XMLMapperBuilder(in, configuration, "mapper/support/FaqMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
    }

    private String listSql(String faqClsfCd, String keyword) {
        Map<String, Object> param = new java.util.HashMap<>();
        param.put("faqClsfCd", faqClsfCd);
        param.put("keyword", keyword);
        String sql = configuration.getMappedStatement("com.nyangmeong.support.mapper.FaqMapper.selectList")
                .getBoundSql(param).getSql();
        return sql.replaceAll("\\s+", " ");
    }

    @Test
    void 조건이_없으면_사용_중인_FAQ를_정렬_순서로_조회한다() {
        String sql = listSql(null, null);

        assertThat(sql).contains("WHERE F.USE_YN = 'Y'");
        assertThat(sql).contains("ORDER BY F.SORT_SEQ ASC, F.FAQ_SN ASC");
        assertThat(sql).doesNotContain("LIKE");
        assertThat(sql).doesNotContain("F.FAQ_CLSF_CD = ?");
    }

    @Test
    void 카테고리만_있으면_코드_조건이_붙는다() {
        String sql = listSql("CSCC0002", null);

        assertThat(sql).contains("AND F.FAQ_CLSF_CD = ?");
        assertThat(sql).doesNotContain("LIKE");
    }

    @Test
    void 키워드만_있으면_질문_또는_답변_LIKE_조건이_붙는다() {
        String sql = listSql(null, "비밀번호");

        assertThat(sql).contains("AND ( F.QSTN_TTL LIKE CONCAT('%', ?, '%') OR F.ANS_CN LIKE CONCAT('%', ?, '%') )");
        assertThat(sql).doesNotContain("F.FAQ_CLSF_CD = ?");
    }

    @Test
    void 카테고리와_키워드를_함께_주면_괄호로_묶인_OR이_AND로_붙는다() {
        String sql = listSql("CSCC0002", "비밀번호");

        assertThat(sql).contains("AND F.FAQ_CLSF_CD = ? AND ( F.QSTN_TTL LIKE CONCAT('%', ?, '%') "
                + "OR F.ANS_CN LIKE CONCAT('%', ?, '%') ) ORDER BY");
    }
}
