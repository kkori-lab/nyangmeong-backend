package com.nyangmeong.community.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.nyangmeong.community.dto.ChatRoomSearchCond;

/**
 * [검사 - 펫돌봄 채팅 SQL]
 * - DB 없이 Mapper XML 에서 만들어지는 SQL 의 필터·정렬·잠금 읽기만 확인
 */
class ChatMessageMapperSqlTest {

    private static Configuration configuration;

    @BeforeAll
    static void loadMappers() throws Exception {
        configuration = new Configuration();
        // 작성자 표시 공통 조각(MemberDisplay)을 먼저 불러와야 include 가 풀린다
        String displayPath = "mapper/global/MemberDisplay.xml";
        try (InputStream in = ChatMessageMapperSqlTest.class.getResourceAsStream("/" + displayPath)) {
            new XMLMapperBuilder(in, configuration, displayPath, configuration.getSqlFragments()).parse();
        }
        for (String name : new String[] { "ChatRoomMapper", "ChatMessageMapper" }) {
            String path = "mapper/community/" + name + ".xml";
            try (InputStream in = ChatMessageMapperSqlTest.class.getResourceAsStream("/" + path)) {
                new XMLMapperBuilder(in, configuration, path, configuration.getSqlFragments()).parse();
            }
        }
    }

    private String sql(String statement, Object param) {
        return configuration.getMappedStatement("com.nyangmeong.community.mapper." + statement)
                .getBoundSql(param).getSql().replaceAll("\\s+", " ");
    }

    private Map<String, Object> listParam(ChatRoomSearchCond cond) {
        Map<String, Object> param = new HashMap<>();
        param.put("mbrSn", 2L);
        param.put("cond", cond);
        param.put("joinedCd", "CHTC0003");
        param.put("guardianRole", "GUARDIAN");
        param.put("sitterRole", "SITTER");
        return param;
    }

    @Test
    void 채팅_목록은_조건이_없으면_내가_참여_중인_방을_마지막_대화_최신순으로_고른다() {
        String sql = sql("ChatRoomMapper.selectList", listParam(new ChatRoomSearchCond()));

        assertThat(sql).contains("ME.MBR_SN = ?").contains("ME.PTCP_STTS_CD = ?")
                .contains("ORDER BY COALESCE(LM.REG_DT, C.REG_DT) DESC")
                .doesNotContain("P.MBR_SN = ? AND").doesNotContain("AND C.PST_SN = ?")
                .doesNotContain("AND EXISTS");
    }

    @Test
    void 안_읽은_수는_내_메시지와_시스템_메시지를_세지_않는다() {
        String sql = sql("ChatRoomMapper.selectList", listParam(new ChatRoomSearchCond()));

        assertThat(sql).contains("UM.MSG_SEQ > ME.LAST_PRSL_MSG_SEQ").contains("UM.MBR_SN IS NOT NULL")
                .contains("UM.MBR_SN <> ?");
        assertThat(sql("ChatRoomMapper.countUnread", listParam(null))).contains("UM.MBR_SN IS NOT NULL")
                .contains("UM.MBR_SN <> ?").contains("UM.MSG_SEQ > ME.LAST_PRSL_MSG_SEQ");
    }

    @Test
    void 탭은_내_글과_지원한_글로_나눈다() {
        ChatRoomSearchCond mine = new ChatRoomSearchCond();
        mine.setTab("MINE");
        ChatRoomSearchCond applied = new ChatRoomSearchCond();
        applied.setTab("APPLIED");

        assertThat(sql("ChatRoomMapper.selectList", listParam(mine))).contains("AND P.MBR_SN = ?")
                .doesNotContain("AND C.CTPT_MBR_SN = ?");
        assertThat(sql("ChatRoomMapper.selectList", listParam(applied))).contains("AND C.CTPT_MBR_SN = ?")
                .doesNotContain("AND P.MBR_SN = ?");
    }

    @Test
    void 안_읽음만과_글_번호는_조건으로_붙는다() {
        ChatRoomSearchCond cond = new ChatRoomSearchCond();
        cond.setUnreadOnly(true);
        cond.setPstSn(7L);

        assertThat(sql("ChatRoomMapper.selectList", listParam(cond))).contains("AND C.PST_SN = ?")
                .contains("AND EXISTS ( SELECT 1 FROM CHAT_MSG UM2");
    }

    @Test
    void 읽음_위치는_앞으로만_가고_방의_마지막_순서를_넘지_않는다() {
        Map<String, Object> param = new HashMap<>();
        param.put("otoChatRoomSn", 11L);
        param.put("mbrSn", 2L);
        param.put("lastPrslMsgSeq", 9);

        assertThat(sql("ChatRoomMapper.updateLastRead", param)).contains("LEAST(")
                .contains("AND LAST_PRSL_MSG_SEQ < LEAST(");
    }

    @Test
    void 방과_메시지_순서는_잠금_읽기로_직렬화한다() {
        Map<String, Object> param = new HashMap<>();
        param.put("otoChatRoomSn", 11L);
        param.put("idmptDmndId", "m-1");

        assertThat(sql("ChatRoomMapper.selectRoomForUpdate", param)).endsWith("FOR UPDATE");
        assertThat(sql("ChatMessageMapper.selectNextMsgSeqForUpdate", param)).contains("MAX(M.MSG_SEQ)")
                .endsWith("FOR UPDATE");
        assertThat(sql("ChatMessageMapper.selectByIdmptDmndIdForShare", param)).endsWith("FOR SHARE");
    }

    @Test
    void 메시지_조회는_커서가_있을_때만_그보다_오래된_것을_고르고_최신순으로_자른다() {
        Map<String, Object> first = new HashMap<>();
        first.put("otoChatRoomSn", 11L);
        first.put("beforeChatMsgSn", null);
        first.put("limit", 31);
        Map<String, Object> next = new HashMap<>(first);
        next.put("beforeChatMsgSn", 40L);

        assertThat(sql("ChatMessageMapper.selectList", first)).doesNotContain("M.CHAT_MSG_SN < ?")
                .contains("ORDER BY M.CHAT_MSG_SN DESC LIMIT ?");
        assertThat(sql("ChatMessageMapper.selectList", next)).contains("AND M.CHAT_MSG_SN < ?");
    }

    @Test
    void 사진은_방의_메시지에_붙은_것만_본다() {
        Map<String, Object> param = new HashMap<>();
        param.put("otoChatRoomSn", 11L);
        param.put("atchFileSn", 600L);

        assertThat(sql("ChatRoomMapper.existsPhoto", param)).contains("M.OTO_CHAT_ROOM_SN = ?")
                .contains("PH.ATCH_FILE_SN = ?");
        Map<String, Object> photos = new HashMap<>();
        photos.put("chatMsgSns", List.of(1L, 2L));
        assertThat(sql("ChatMessageMapper.selectPhotos", photos)).contains("IN ( ? , ? )")
                .contains("ORDER BY PH.CHAT_MSG_SN, PH.SORT_SEQ");
    }
}
