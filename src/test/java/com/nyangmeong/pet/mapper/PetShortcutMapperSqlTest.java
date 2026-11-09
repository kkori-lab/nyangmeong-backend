package com.nyangmeong.pet.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.HashMap;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * [검사 - 반려동물 메인 바로가기 SQL]
 * - DB 없이 대상 반려동물 조건, 행 정렬, 저장 덮어쓰기만 확인
 */
class PetShortcutMapperSqlTest {

    private static Configuration configuration;

    @BeforeAll
    static void loadMapper() throws Exception {
        configuration = new Configuration();
        String path = "mapper/pet/PetShortcutMapper.xml";
        try (InputStream in = PetShortcutMapperSqlTest.class.getResourceAsStream("/" + path)) {
            new XMLMapperBuilder(in, configuration, path, configuration.getSqlFragments()).parse();
        }
    }

    private String sql(String statement) {
        return configuration.getMappedStatement("com.nyangmeong.pet.mapper.PetShortcutMapper." + statement)
                .getBoundSql(new HashMap<String, Object>()).getSql().replaceAll("\\s+", " ").trim();
    }

    @Test
    void 대상은_양육_중이고_삭제되지_않은_반려동물이다() {
        assertThat(sql("selectCaregivingPetSns")).contains("R.MBR_SN = ?").contains("R.USE_YN = 'Y'")
                .contains("P.DEL_YN = 'N'");
    }

    @Test
    void 행은_반려동물별_순서대로_읽는다() {
        assertThat(sql("selectList")).contains("WHERE MBR_SN = ?")
                .endsWith("ORDER BY PET_SN, SORT_SEQ, PET_MAIN_SHRTC_SN");
    }

    @Test
    void 저장은_있으면_표시_여부와_순서만_바꾼다() {
        assertThat(sql("upsert"))
                .contains("INSERT INTO PET_MAIN_SHRTC (MBR_SN, PET_SN, SHRTC_CD, INDCT_YN, SORT_SEQ)")
                .endsWith("ON DUPLICATE KEY UPDATE INDCT_YN = VALUES(INDCT_YN), SORT_SEQ = VALUES(SORT_SEQ)");
    }
}
