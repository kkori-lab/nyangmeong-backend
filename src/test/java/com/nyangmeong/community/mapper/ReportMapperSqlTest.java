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
 * [검사 - 채팅 상대 신고 SQL]
 * - DB 없이 Mapper XML 에서 만들어지는 SQL 확인
 * - 누적 자동 정지는 신고 건수가 아니라 신고자 수 : 같은 신고자가 다른 방에서 신고해도 1명
 * - 글·댓글 대상 쿼리가 회원 신고를 댓글로 잘못 읽지 않음
 */
class ReportMapperSqlTest {

    private static Configuration configuration;

    @BeforeAll
    static void loadMapper() throws Exception {
        configuration = new Configuration();
        String path = "mapper/community/ReportMapper.xml";
        try (InputStream in = ReportMapperSqlTest.class.getResourceAsStream("/" + path)) {
            new XMLMapperBuilder(in, configuration, path, configuration.getSqlFragments()).parse();
        }
    }

    private String sql(String statement, Map<String, Object> param) {
        return configuration.getMappedStatement("com.nyangmeong.community.mapper.ReportMapper." + statement)
                .getBoundSql(param).getSql().replaceAll("\\s+", " ");
    }

    @Test
    void 같은_신고자가_다른_방에서_신고해도_1명으로_센다() {
        Map<String, Object> param = new HashMap<>();
        param.put("trgtMbrSn", 20L);
        param.put("status", "CMUC0015");

        String sql = sql("countReportersOfMember", param);

        assertThat(sql).contains("COUNT(DISTINCT MBR_SN)").contains("TRGT_MBR_SN = ?")
                .contains("DCLR_PRCS_STTS_CD = ?").doesNotContain("OTO_CHAT_ROOM_SN");
    }

    @Test
    void 같은_방_중복은_신고자와_채팅방으로_찾는다() {
        Map<String, Object> param = new HashMap<>();
        param.put("mbrSn", 1L);
        param.put("otoChatRoomSn", 40L);

        assertThat(sql("existsChatReport", param)).contains("MBR_SN = ?").contains("OTO_CHAT_ROOM_SN = ?");
    }

    @Test
    void 글도_댓글도_아니면_글_댓글_대상_쿼리는_아무_행도_잡지_않는다() {
        Map<String, Object> param = new HashMap<>();
        param.put("mbrSn", 1L);
        param.put("pstSn", null);
        param.put("cmntSn", null);
        param.put("status", "CMUC0015");
        param.put("rejectedCd", "CMUC0017");

        for (String statement : new String[] { "existsReport", "countReports", "countAccumulatedReports",
                "selectReportsByStatus" }) {
            assertThat(sql(statement, param)).as(statement).contains("1 = 0").doesNotContain("CMNT_SN = ?");
        }
    }

    @Test
    void 같은_대상_신고_목록은_회원_신고를_같은_회원으로_묶는다() {
        Map<String, Object> param = new HashMap<>();
        param.put("pstSn", null);
        param.put("cmntSn", null);
        param.put("trgtMbrSn", 20L);

        assertThat(sql("selectSameTargetList", param)).contains("D.TRGT_MBR_SN = ?")
                .contains("S.TRGT_MBR_SN <=> D.TRGT_MBR_SN");
    }

    @Test
    void 관리자_목록의_회원_필터와_대상_내용은_방의_돌봄_글_제목이다() {
        Map<String, Object> cond = new HashMap<>();
        Map<String, Object> param = new HashMap<>();
        param.put("cond", cond);
        param.put("targetType", "MEMBER");
        param.put("blindCd", "CMUC0003");
        param.put("autoBlindRsn", "신고 누적");
        param.put("autoDeleteRsn", "신고 누적 자동 삭제");

        assertThat(sql("selectAdminList", param)).contains("WHERE D.TRGT_MBR_SN IS NOT NULL")
                .contains("COALESCE(P.PST_TTL, C.CMNT_CN, RP.PST_TTL) AS TRGT_CN")
                .contains("LEFT JOIN OTO_CHAT_ROOM R ON R.OTO_CHAT_ROOM_SN = D.OTO_CHAT_ROOM_SN");
    }
}
