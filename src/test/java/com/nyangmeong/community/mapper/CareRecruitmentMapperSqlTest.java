package com.nyangmeong.community.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * [검사 - 펫돌봄 모집 SQL]
 * - DB 없이 Mapper XML 에서 만들어지는 SQL 에 필터 조건이 붙는지, 공개 조회에 상세 주소·좌표가 없는지만 확인
 * - 작성자·매칭 돌보미 닉네임이 파기 회원 표시 공통 조각(MemberDisplay)을 쓰는지, 관리자 매칭 조회는 원래 닉네임인지 확인
 */
class CareRecruitmentMapperSqlTest {

    /** 파기 회원(이메일 HMAC 이 PURGED 로 시작)은 「탈퇴한 회원」·사진 없음 : MemberDisplay.xml 이 만드는 SQL */
    private static final String PURGED_NCNM = "CASE WHEN M.MBR_STTS_CD = 'USRC0003' AND M.ACNT_EML_HMAC LIKE 'PURGED%' THEN '탈퇴한 회원' ELSE M.MBR_NCNM END";
    private static final String PURGED_PHOTO = "CASE WHEN M.MBR_STTS_CD = 'USRC0003' AND M.ACNT_EML_HMAC LIKE 'PURGED%' THEN NULL ELSE M.ATCH_FILE_SN END";

    private static Configuration configuration;

    @BeforeAll
    static void loadMappers() throws Exception {
        configuration = new Configuration();
        // 작성자 표시 공통 조각(MemberDisplay)을 먼저 불러와야 include 가 풀린다
        for (String name : new String[] { "global/MemberDisplay", "community/CareRecruitmentMapper",
                "community/ChatRoomMapper" }) {
            String path = "mapper/" + name + ".xml";
            try (InputStream in = CareRecruitmentMapperSqlTest.class.getResourceAsStream("/" + path)) {
                new XMLMapperBuilder(in, configuration, path, configuration.getSqlFragments()).parse();
            }
        }
    }

    private String sql(String statement, Map<String, Object> param) {
        return configuration.getMappedStatement("com.nyangmeong.community.mapper." + statement)
                .getBoundSql(param).getSql().replaceAll("\\s+", " ");
    }

    private Map<String, Object> listParam() {
        Map<String, Object> param = new HashMap<>();
        param.put("mbrSn", 1L);
        param.put("pstSttsCd", "CMUC0003");
        param.put("dogCd", "PETC0001");
        param.put("mineYn", false);
        param.put("sizeFilter", false);
        return param;
    }

    @Test
    void 목록은_조건이_없으면_정상_글만_모집_상태순_같은_상태는_최신순으로_고른다() {
        String sql = sql("CareRecruitmentMapper.selectList", listParam());

        assertThat(sql).contains("P.PST_STTS_CD = ?")
                .containsPattern("ORDER BY R\\.RCRT_STTS_CD\\s*,\\s*P\\.REG_DT DESC\\s*,\\s*R\\.PET_PRTC_RCRT_SN DESC")
                .doesNotContain("R.RCRT_STTS_CD = ?").doesNotContain("RCRT_PET_KND").doesNotContain("PRTC_END_DT >=");
    }

    @Test
    void 목록은_조건을_모두_AND로_건다() {
        Map<String, Object> param = listParam();
        param.put("rcrtSttsCd", "PCAC0001");
        param.put("mineYn", true);
        param.put("rgnSn", 3L);
        param.put("bgngDt", java.time.LocalDateTime.of(2026, 11, 10, 0, 0));
        param.put("endDt", java.time.LocalDateTime.of(2026, 11, 13, 0, 0));
        param.put("petSeCd", "PETC0001");
        param.put("sizeFilter", true);
        param.put("minBdwg", new BigDecimal("10"));
        param.put("maxBdwg", new BigDecimal("25"));

        String sql = sql("CareRecruitmentMapper.selectList", param);

        assertThat(sql).contains("R.RCRT_STTS_CD = ?").contains("P.MBR_SN = ?").contains("WITH RECURSIVE")
                .contains("R.PRTC_END_DT >= ?").contains("R.PRTC_BGNG_DT < ?").contains("K.PET_SE_CD = ?")
                .contains("K.PET_BDWG >= ?").contains("K.PET_BDWG < ?");
    }

    @Test
    void 대형은_상한_조건을_붙이지_않는다() {
        Map<String, Object> param = listParam();
        param.put("sizeFilter", true);
        param.put("minBdwg", new BigDecimal("25"));

        assertThat(sql("CareRecruitmentMapper.selectList", param)).contains("K.PET_BDWG >= ?")
                .doesNotContain("K.PET_BDWG < ?");
    }

    @Test
    void 목록은_대표_사진을_고르고_삭제된_사진은_뺀다() {
        assertThat(sql("CareRecruitmentMapper.selectList", listParam())).contains("I.DEL_YN = 'N'")
                .contains("LIMIT 1");
    }

    @Test
    void 목록과_상세는_작성자_사진과_메시지가_오간_채팅방_수를_고른다() {
        String list = sql("CareRecruitmentMapper.selectList", listParam());
        Map<String, Object> param = new HashMap<>();
        param.put("petPrtcRcrtSn", 5L);
        param.put("pstSttsCd", "CMUC0003");
        String detail = sql("CareRecruitmentMapper.selectDetail", param);

        for (String sql : List.of(list, detail)) {
            assertThat(sql).contains(PURGED_PHOTO + " AS MBR_ATCH_FILE_SN")
                    .contains("EXISTS (SELECT 1 FROM CHAT_MSG G WHERE G.OTO_CHAT_ROOM_SN = C.OTO_CHAT_ROOM_SN)");
        }
    }

    @Test
    void 목록은_상세_주소와_좌표를_고르지_않는다() {
        assertThat(sql("CareRecruitmentMapper.selectList", listParam())).doesNotContain("PRTC_PLC_DADDR_ENC")
                .doesNotContain("PRTC_PLC_LAT").doesNotContain("PRTC_PLC_LOT");
    }

    @Test
    void 모집_수정은_모집중_상태만_바꾼다() {
        Map<String, Object> param = new HashMap<>();
        param.put("recruitment", new com.nyangmeong.community.domain.CareRecruitment());
        param.put("recruitingCd", "PCAC0001");

        assertThat(sql("CareRecruitmentMapper.updateRecruitment", param)).contains("AND RCRT_STTS_CD = ?")
                .doesNotContain("SET RCRT_STTS_CD").doesNotContain("UTZTN_RUL_AGRE_YN");
    }

    @Test
    void 중복_키_뒤_다시_조회는_잠금_읽기다() {
        Map<String, Object> param = new HashMap<>();
        param.put("pstSn", 7L);
        param.put("ctptMbrSn", 2L);

        assertThat(sql("ChatRoomMapper.selectByPstSnAndCtptMbrSnForShare", param)).endsWith("FOR SHARE");
        assertThat(sql("ChatRoomMapper.selectByPstSnAndCtptMbrSn", param)).doesNotContain("FOR SHARE");
        assertThat(sql("ChatRoomMapper.selectDetailForShare", param)).endsWith("FOR SHARE")
                .contains("FROM OTO_CHAT_ROOM C");
        assertThat(sql("ChatRoomMapper.selectDetail", param)).doesNotContain("FOR SHARE");
    }

    @Test
    void 채팅방은_게시글과_지원자_조합으로_찾는다() {
        Map<String, Object> param = new HashMap<>();
        param.put("pstSn", 7L);
        param.put("ctptMbrSn", 2L);

        assertThat(sql("ChatRoomMapper.selectByPstSnAndCtptMbrSn", param)).contains("C.PST_SN = ?")
                .contains("C.CTPT_MBR_SN = ?");
    }

    @Test
    void 채팅_중_여부는_나가지_않은_참여_방_중_메시지가_오간_방만_본다() {
        Map<String, Object> param = new HashMap<>();
        param.put("pstSn", 7L);
        param.put("mbrSn", 2L);
        param.put("joinedCd", "CHTC0003");

        assertThat(sql("CareRecruitmentMapper.existsChatting", param)).startsWith("SELECT EXISTS")
                .contains("C.PST_SN = ?").contains("T.MBR_SN = ?").contains("T.PTCP_STTS_CD = ?")
                .contains("FROM CHAT_MSG G WHERE G.OTO_CHAT_ROOM_SN = C.OTO_CHAT_ROOM_SN");
    }

    @Test
    void 매칭_돌보미는_확정된_매칭의_채팅방_지원자_닉네임이다() {
        Map<String, Object> param = new HashMap<>();
        param.put("petPrtcMtchSn", 30L);

        assertThat(sql("CareRecruitmentMapper.selectMatchedSitterNcnm", param))
                .contains("SELECT " + PURGED_NCNM + " FROM PET_PRTC_MTCH")
                .contains("M.MBR_SN = C.CTPT_MBR_SN").contains("T.PET_PRTC_MTCH_SN = ?");
    }

    @Test
    void 목록은_작성자_닉네임과_사진을_공통_조각으로_고른다() {
        assertThat(sql("CareRecruitmentMapper.selectList", listParam())).contains(PURGED_NCNM + " AS MBR_NCNM")
                .contains(PURGED_PHOTO + " AS MBR_ATCH_FILE_SN");
    }

    @Test
    void 상세는_작성자_닉네임과_사진을_공통_조각으로_고른다() {
        Map<String, Object> param = new HashMap<>();
        param.put("petPrtcRcrtSn", 5L);
        param.put("pstSttsCd", "CMUC0003");

        assertThat(sql("CareRecruitmentMapper.selectDetail", param)).contains(PURGED_NCNM + " AS MBR_NCNM")
                .contains(PURGED_PHOTO + " AS MBR_ATCH_FILE_SN");
    }

    @Test
    void 글_번호로_고르는_상세도_공통_조각을_쓴다() {
        Map<String, Object> param = new HashMap<>();
        param.put("pstSn", 7L);
        param.put("pstSttsCd", "CMUC0003");

        assertThat(sql("CareRecruitmentMapper.selectDetailByPstSn", param)).contains(PURGED_NCNM + " AS MBR_NCNM")
                .contains(PURGED_PHOTO + " AS MBR_ATCH_FILE_SN");
    }

    @Test
    void 관리자_매칭_조회는_원래_닉네임을_그대로_준다() {
        assertThat(sql("CareRecruitmentMapper.selectAdminList", new HashMap<>())).doesNotContain("탈퇴한 회원");
        assertThat(sql("CareRecruitmentMapper.selectAdminDetail", new HashMap<>())).doesNotContain("탈퇴한 회원");
    }
}
