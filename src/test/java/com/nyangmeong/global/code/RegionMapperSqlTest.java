package com.nyangmeong.global.code;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * [검사 - 지역 목록 SQL]
 * - DB 없이 Mapper XML 에서 만들어지는 SQL 에 전북만 거르는 조건이 있는지만 확인
 */
class RegionMapperSqlTest {

    private static Configuration configuration;

    @BeforeAll
    static void loadMapper() throws Exception {
        configuration = new Configuration();
        String path = "mapper/global/RegionMapper.xml";
        try (InputStream in = RegionMapperSqlTest.class.getResourceAsStream("/" + path)) {
            new XMLMapperBuilder(in, configuration, path, configuration.getSqlFragments()).parse();
        }
    }

    private String selectList(Long upRgnSn) {
        Map<String, Object> param = new HashMap<>();
        param.put("upRgnSn", upRgnSn);
        return configuration.getMappedStatement("com.nyangmeong.global.code.RegionMapper.selectList")
                .getBoundSql(param).getSql().replaceAll("\\s+", " ");
    }

    @Test
    void 상위_지역이_없으면_전북_시도만_고른다() {
        assertThat(selectList(null)).contains("ADMDST_CD LIKE '52%'").contains("RGN_STP_CD = 'RGNC0001'")
                .doesNotContain("UP_RGN_SN = ?");
    }

    @Test
    void 상위_지역이_있어도_전북_하위만_고른다() {
        assertThat(selectList(80L)).contains("ADMDST_CD LIKE '52%'").contains("UP_RGN_SN = ?")
                .doesNotContain("RGN_STP_CD = 'RGNC0001'");
    }

    @Test
    void 사용_중인_지역만_사용_순서로_고른다() {
        assertThat(selectList(null)).contains("USE_YN = 'Y'").endsWith("ORDER BY SORT_SEQ , RGN_SN");
    }
}
