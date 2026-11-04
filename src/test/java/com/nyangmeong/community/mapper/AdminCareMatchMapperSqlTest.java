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
 * [검사 - 관리자 펫돌봄 거래 SQL]
 * - DB 없이 Mapper XML 에서 만들어지는 SQL 의 상태 조건·정렬, 채팅 메시지 미조회만 확인
 */
class AdminCareMatchMapperSqlTest {

    private static Configuration configuration;

    @BeforeAll
    static void loadMappers() throws Exception {
        configuration = new Configuration();
        for (String name : new String[] { "CareRecruitmentMapper", "CareMatchMapper" }) {
            String path = "mapper/community/" + name + ".xml";
            try (InputStream in = AdminCareMatchMapperSqlTest.class.getResourceAsStream("/" + path)) {
                new XMLMapperBuilder(in, configuration, path, configuration.getSqlFragments()).parse();
            }
        }
    }

    private String sql(String statement, Map<String, Object> param) {
        return configuration.getMappedStatement("com.nyangmeong.community.mapper." + statement)
                .getBoundSql(param).getSql().replaceAll("\\s+", " ").strip();
    }

    private String listSql(String rcrtSttsCd) {
        Map<String, Object> param = new HashMap<>();
        param.put("rcrtSttsCd", rcrtSttsCd);
        param.put("pstSttsCd", "CMUC0003");
        return sql("CareRecruitmentMapper.selectAdminList", param);
    }

    private String matchSql(String statement) {
        Map<String, Object> param = new HashMap<>();
        param.put("petPrtcRcrtSn", 1L);
        return sql("CareMatchMapper." + statement, param);
    }

    @Test
    void 목록은_정상_글만_최신순() {
        assertThat(listSql(null)).contains("WHERE P.PST_STTS_CD = ?").doesNotContain("R.RCRT_STTS_CD = ?")
                .endsWith("ORDER BY P.REG_DT DESC , R.PET_PRTC_RCRT_SN DESC");
    }

    @Test
    void 목록_상태_필터는_모집_상태_조건을_붙인다() {
        assertThat(listSql("PCAC0002")).contains("AND R.RCRT_STTS_CD = ?");
    }

    @Test
    void 상세는_삭제_글만_뺀다() {
        Map<String, Object> param = new HashMap<>();
        param.put("petPrtcRcrtSn", 1L);
        param.put("deletedCd", "CMUC0005");
        assertThat(sql("CareRecruitmentMapper.selectAdminDetail", param))
                .contains("R.PET_PRTC_RCRT_SN = ? AND P.PST_STTS_CD <> ?");
    }

    @Test
    void 매칭_쿼리는_모집글_기준이고_채팅_메시지를_읽지_않는다() {
        for (String statement : new String[] { "selectAdminMatches", "selectAdminConditionVersions",
                "selectAdminStatusHistories", "selectAdminCompletionRequests", "selectAdminCompletionPhotos" }) {
            assertThat(matchSql(statement)).contains("T.PET_PRTC_RCRT_SN = ?").doesNotContain("CHAT_MSG")
                    .doesNotContain("OTO_CHAT_ROOM_SN AS");
        }
    }

    @Test
    void 정렬은_매칭_최신순_버전_내림차순_이력_오래된_순_회차_순() {
        assertThat(matchSql("selectAdminMatches")).endsWith("ORDER BY T.REG_DT DESC , T.PET_PRTC_MTCH_SN DESC");
        assertThat(matchSql("selectAdminConditionVersions")).endsWith("V.CND_VER_NO DESC");
        assertThat(matchSql("selectAdminStatusHistories"))
                .endsWith("ORDER BY H.PET_PRTC_MTCH_SN , H.REG_DT , H.PET_PRTC_MTCH_STTS_HSTRY_SN");
        assertThat(matchSql("selectAdminCompletionRequests")).endsWith("D.DMND_CYCL");
    }
}
