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
 * [검사 - 관리자 실종제보 목록 SQL]
 * - DB 없이 Mapper XML 에서 만들어지는 SQL 의 상태 조건·정렬만 확인
 */
class LostReportMapperSqlTest {

    private static final String MISSING = "CMUC0006";
    private static final String FOUND = "CMUC0007";
    private static final String EXPIRED = "CMUC0008";

    private static Configuration configuration;

    @BeforeAll
    static void loadMapper() throws Exception {
        configuration = new Configuration();
        // 작성자 표시 공통 조각(MemberDisplay)을 먼저 불러와야 include 가 풀린다
        for (String path : new String[] { "mapper/global/MemberDisplay.xml", "mapper/community/LostReportMapper.xml" }) {
            try (InputStream in = LostReportMapperSqlTest.class.getResourceAsStream("/" + path)) {
                new XMLMapperBuilder(in, configuration, path, configuration.getSqlFragments()).parse();
            }
        }
    }

    private String whereOf(String sttsCd) {
        String sql = adminListSql(sttsCd);
        return sql.substring(sql.indexOf("WHERE"));
    }

    private String adminListSql(String sttsCd) {
        Map<String, Object> param = new HashMap<>();
        param.put("lostTpofSttsCd", sttsCd);
        param.put("pstSttsCd", "CMUC0003");
        param.put("missingCd", MISSING);
        param.put("foundCd", FOUND);
        param.put("expiredCd", EXPIRED);
        return configuration.getMappedStatement("com.nyangmeong.community.mapper.LostReportMapper.selectAdminList")
                .getBoundSql(param).getSql().replaceAll("\\s+", " ").strip();
    }

    @Test
    void 전체는_상태_조건_없이_최신순() {
        assertThat(whereOf(null)).doesNotContain("LOST_TPOF_STTS_CD").contains("P.PST_STTS_CD = ?")
                .endsWith("ORDER BY P.REG_DT DESC , L.LOST_TPOF_SN DESC");
    }

    @Test
    void 만료는_기간_지난_실종을_포함한다() {
        assertThat(whereOf(EXPIRED)).contains(
                "(L.LOST_TPOF_STTS_CD = ? OR (L.LOST_TPOF_STTS_CD = ? AND L.EXPRY_CRTR_DT < NOW()))");
    }

    @Test
    void 실종은_기간_안의_글만() {
        assertThat(whereOf(MISSING)).contains("L.LOST_TPOF_STTS_CD = ? AND L.EXPRY_CRTR_DT >= NOW()");
    }

    @Test
    void 찾았어요는_코드_조건과_만료_기준일_null() {
        assertThat(adminListSql(FOUND))
                .contains("CASE WHEN L.LOST_TPOF_STTS_CD = ? THEN NULL ELSE L.EXPRY_CRTR_DT END");
        assertThat(whereOf(FOUND)).contains("AND L.LOST_TPOF_STTS_CD = ?").doesNotContain("EXPRY_CRTR_DT");
    }

    @Test
    void 만료_사전_알림_대상은_정상_실종_글의_종료일_날짜로_고른다() {
        Map<String, Object> param = new HashMap<>();
        param.put("missingCd", MISSING);
        param.put("pstSttsCd", "CMUC0003");
        param.put("days", 7);
        param.put("lostTpofSn", 1L);
        String expected = "P.PST_STTS_CD = ? AND L.LOST_TPOF_STTS_CD = ? AND L.EXPRY_CRTR_DT >= NOW()"
                + " AND DATE(L.EXPRY_CRTR_DT) = DATE_ADD(CURDATE(), INTERVAL ? DAY)";
        for (String id : new String[] { "selectExpiryAlarmTargets", "existsExpiryAlarmTarget" }) {
            String sql = configuration.getMappedStatement("com.nyangmeong.community.mapper.LostReportMapper." + id)
                    .getBoundSql(param).getSql().replaceAll("\\s+", " ").strip();
            assertThat(sql).contains(expected).contains("JOIN PST P ON P.PST_SN = L.PST_SN");
        }
    }
}
