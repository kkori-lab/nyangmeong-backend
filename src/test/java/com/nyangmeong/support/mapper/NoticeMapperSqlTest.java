package com.nyangmeong.support.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * [검사 - 공지사항 목록 SQL]
 * - DB 없이 Mapper XML 에서 만들어지는 SQL 의 조건·정렬만 확인
 */
class NoticeMapperSqlTest {

    private static Configuration configuration;

    @BeforeAll
    static void loadMapper() throws Exception {
        configuration = new Configuration();
        try (InputStream in = NoticeMapperSqlTest.class.getResourceAsStream("/mapper/support/NoticeMapper.xml")) {
            new XMLMapperBuilder(in, configuration, "mapper/support/NoticeMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
    }

    private String listSql(String keyword, boolean latest) {
        Map<String, Object> param = new java.util.HashMap<>();
        param.put("keyword", keyword);
        param.put("latest", latest);
        String sql = configuration.getMappedStatement("com.nyangmeong.support.mapper.NoticeMapper.selectList")
                .getBoundSql(param).getSql();
        return sql.replaceAll("\\s+", " ");
    }

    @Test
    void 기본_목록은_상단_고정_우선_작성일_최신순() {
        String sql = listSql(null, false);

        assertThat(sql).contains("WHERE N.USE_YN = 'Y'");
        assertThat(sql).contains("ORDER BY N.UPEND_FIX_YN DESC, N.REG_DT DESC, N.NTC_MTTR_SN DESC");
        assertThat(sql).doesNotContain("LIKE");
    }

    @Test
    void latest면_고정을_무시하고_작성일_최신순() {
        String sql = listSql(null, true);

        assertThat(sql).contains("ORDER BY N.REG_DT DESC, N.NTC_MTTR_SN DESC");
        assertThat(sql).doesNotContain("UPEND_FIX_YN DESC");
    }

    @Test
    void 검색어가_있으면_제목_LIKE_조건이_붙는다() {
        String sql = listSql("점검", false);

        assertThat(sql).contains("AND N.NTC_MTTR_TTL LIKE CONCAT('%', ?, '%')");
    }
}
