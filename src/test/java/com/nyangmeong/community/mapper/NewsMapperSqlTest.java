package com.nyangmeong.community.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * [검사 - 뉴스 목록 SQL]
 * - DB 없이 Mapper XML 에서 만들어지는 SQL 의 조건·정렬만 확인
 */
class NewsMapperSqlTest {

    private static Configuration configuration;

    @BeforeAll
    static void loadMapper() throws Exception {
        configuration = new Configuration();
        try (InputStream in = NewsMapperSqlTest.class.getResourceAsStream("/mapper/community/NewsMapper.xml")) {
            new XMLMapperBuilder(in, configuration, "mapper/community/NewsMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
    }

    private String listSql(String newsClsfCd, String keyword) {
        Map<String, Object> param = new HashMap<>();
        param.put("newsClsfCd", newsClsfCd);
        param.put("keyword", keyword);
        String sql = configuration.getMappedStatement("com.nyangmeong.community.mapper.NewsMapper.selectList")
                .getBoundSql(param).getSql();
        return sql.replaceAll("\\s+", " ");
    }

    @Test
    void 조건이_없으면_사용_중인_뉴스를_최신순으로_조회한다() {
        String sql = listSql(null, null);

        assertThat(sql).contains("WHERE N.USE_YN = 'Y'");
        assertThat(sql).contains("ORDER BY N.PBLCN_DT DESC, N.NEWS_SN DESC");
        assertThat(sql).contains("LEFT(N.NEWS_CN, 200) AS NEWS_CN");
        assertThat(sql).doesNotContain("LIKE");
        assertThat(sql).doesNotContain("N.NEWS_CLSF_CD = ?");
    }

    @Test
    void 카테고리만_있으면_코드_조건이_붙는다() {
        String sql = listSql("CMUC0018", null);

        assertThat(sql).contains("AND N.NEWS_CLSF_CD = ?");
        assertThat(sql).doesNotContain("LIKE");
    }

    @Test
    void 키워드만_있으면_제목_또는_내용_LIKE_조건이_붙는다() {
        String sql = listSql(null, "입양");

        assertThat(sql).contains("AND ( N.NEWS_TTL LIKE CONCAT('%', ?, '%') OR N.NEWS_CN LIKE CONCAT('%', ?, '%') )");
        assertThat(sql).doesNotContain("N.NEWS_CLSF_CD = ?");
    }

    @Test
    void 카테고리와_키워드를_함께_주면_괄호로_묶인_OR이_AND로_붙는다() {
        String sql = listSql("CMUC0018", "입양");

        assertThat(sql).contains("AND N.NEWS_CLSF_CD = ? AND ( N.NEWS_TTL LIKE CONCAT('%', ?, '%') "
                + "OR N.NEWS_CN LIKE CONCAT('%', ?, '%') ) ORDER BY");
    }

    @Test
    void 분류_키워드는_뉴스_분류_용도의_삭제되지_않은_행만_정렬_순서로_읽는다() {
        String sql = configuration.getMappedStatement("com.nyangmeong.community.mapper.NewsMapper.selectKeywords")
                .getBoundSql(new HashMap<>()).getSql().replaceAll("\\s+", " ");

        assertThat(sql).contains("K.DICT_USG_CD = 'AIMC0039'");
        assertThat(sql).contains("K.USE_YN = 'Y'");
        assertThat(sql).contains("K.DEL_YN = 'N'");
        assertThat(sql).contains("ORDER BY K.SORT_SEQ ASC, K.KWRD_DICT_SN ASC");
    }
}
