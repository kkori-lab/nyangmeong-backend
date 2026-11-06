package com.nyangmeong.community.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.nyangmeong.community.dto.ChatRoomSearchCond;

/**
 * [검사 - 채팅방 상대 닉네임·프로필 사진 SQL]
 * - DB 없이 Mapper XML 에서 만들어지는 SQL 이 파기 회원 표시 공통 조각(MemberDisplay)을 쓰는지 확인
 * - 채팅은 회원 두 명(글쓴이 O, 지원자 A)을 함께 읽으므로 별칭마다 조각이 들어가는지 본다
 */
class ChatRoomMapperSqlTest {

    private static Configuration configuration;

    @BeforeAll
    static void loadMappers() throws Exception {
        configuration = new Configuration();
        for (String name : new String[] { "global/MemberDisplay", "community/ChatRoomMapper" }) {
            String path = "mapper/" + name + ".xml";
            try (InputStream in = ChatRoomMapperSqlTest.class.getResourceAsStream("/" + path)) {
                new XMLMapperBuilder(in, configuration, path, configuration.getSqlFragments()).parse();
            }
        }
    }

    /** 파기 회원(이메일 HMAC 이 PURGED 로 시작)은 「탈퇴한 회원」 : MemberDisplay.xml 이 만드는 SQL */
    private static String purgedNcnm(String alias) {
        return "CASE WHEN " + alias + ".MBR_STTS_CD = 'USRC0003' AND " + alias
                + ".ACNT_EML_HMAC LIKE 'PURGED%' THEN '탈퇴한 회원' ELSE " + alias + ".MBR_NCNM END";
    }

    /** 파기 회원은 사진 없음 */
    private static String purgedPhoto(String alias) {
        return "CASE WHEN " + alias + ".MBR_STTS_CD = 'USRC0003' AND " + alias
                + ".ACNT_EML_HMAC LIKE 'PURGED%' THEN NULL ELSE " + alias + ".ATCH_FILE_SN END";
    }

    private String sql(String statement, Map<String, Object> param) {
        return configuration.getMappedStatement("com.nyangmeong.community.mapper.ChatRoomMapper." + statement)
                .getBoundSql(param).getSql().replaceAll("\\s+", " ");
    }

    @Test
    void 채팅_목록은_상대_닉네임과_사진을_두_분기_모두_공통_조각으로_고른다() {
        Map<String, Object> param = new HashMap<>();
        param.put("mbrSn", 2L);
        param.put("cond", new ChatRoomSearchCond());
        param.put("joinedCd", "CHTC0003");
        param.put("guardianRole", "GUARDIAN");
        param.put("sitterRole", "SITTER");

        String sql = sql("selectList", param);

        assertThat(sql)
                .contains("THEN " + purgedNcnm("A") + " ELSE " + purgedNcnm("O") + " END AS CTPT_MBR_NCNM")
                .contains("THEN " + purgedPhoto("A") + " ELSE " + purgedPhoto("O") + " END AS CTPT_ATCH_FILE_SN");
    }

    @Test
    void 채팅방_조회는_글쓴이와_지원자_닉네임과_사진을_공통_조각으로_고른다() {
        Map<String, Object> param = new HashMap<>();
        param.put("otoChatRoomSn", 5L);

        for (String statement : new String[] { "selectDetail", "selectDetailForShare" }) {
            assertThat(sql(statement, param)).contains(purgedNcnm("O") + " AS OWNER_NCNM")
                    .contains(purgedPhoto("O") + " AS OWNER_ATCH_FILE_SN")
                    .contains(purgedNcnm("A") + " AS APPLICANT_NCNM")
                    .contains(purgedPhoto("A") + " AS APPLICANT_ATCH_FILE_SN");
        }
    }
}
