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
 * [검사 - 작성자 닉네임·프로필 사진 SQL]
 * - DB 없이 Mapper XML 에서 만들어지는 SQL 에 작성자 사진 번호가 있는지 확인
 * - 펫이야기 사용자 화면 쿼리가 파기 회원 표시 공통 조각(MemberDisplay)을 쓰는지 확인
 * - 탈퇴했지만 파기 전인 회원은 원래 닉네임 그대로여야 하므로 회원 상태 조건은 없어야 한다
 */
class PostMapperSqlTest {

    /** 파기 회원(이메일 HMAC 이 PURGED 로 시작)은 「탈퇴한 회원」·사진 없음 : MemberDisplay.xml 이 만드는 SQL */
    private static final String PURGED_NCNM = "CASE WHEN M.MBR_STTS_CD = 'USRC0003' AND M.ACNT_EML_HMAC LIKE 'PURGED%' THEN '탈퇴한 회원' ELSE M.MBR_NCNM END";
    private static final String PURGED_PHOTO = "CASE WHEN M.MBR_STTS_CD = 'USRC0003' AND M.ACNT_EML_HMAC LIKE 'PURGED%' THEN NULL ELSE M.ATCH_FILE_SN END";

    private static Configuration configuration;

    @BeforeAll
    static void loadMappers() throws Exception {
        configuration = new Configuration();
        for (String name : new String[] { "global/MemberDisplay", "community/PostMapper", "community/CommentMapper",
                "community/LostReportMapper" }) {
            String path = "mapper/" + name + ".xml";
            try (InputStream in = PostMapperSqlTest.class.getResourceAsStream("/" + path)) {
                new XMLMapperBuilder(in, configuration, path, configuration.getSqlFragments()).parse();
            }
        }
    }

    private String sql(String statement) {
        Map<String, Object> param = new HashMap<>();
        return configuration.getMappedStatement("com.nyangmeong.community.mapper." + statement)
                .getBoundSql(param).getSql().replaceAll("\\s+", " ");
    }

    @Test
    void 게시글_목록은_작성자_사진_번호를_고른다() {
        assertThat(sql("PostMapper.selectList")).contains(PURGED_PHOTO + " AS MBR_ATCH_FILE_SN")
                .contains(PURGED_NCNM + " AS MBR_NCNM");
    }

    @Test
    void 오늘의_건강팁은_작성자_사진_번호를_고른다() {
        assertThat(sql("PostMapper.selectHealthTips")).contains(PURGED_PHOTO + " AS MBR_ATCH_FILE_SN")
                .contains(PURGED_NCNM + " AS MBR_NCNM");
    }

    @Test
    void 작성자_조회는_닉네임과_사진_번호를_함께_고른다() {
        assertThat(sql("PostMapper.selectAuthor")).contains(PURGED_NCNM + " AS mbrNcnm")
                .contains(PURGED_PHOTO + " AS mbrAtchFileSn");
    }

    @Test
    void 댓글_목록은_삭제_댓글이면_사진_번호를_비운다() {
        assertThat(sql("CommentMapper.selectList"))
                .contains("CASE WHEN C.DEL_YN = 'Y' THEN NULL ELSE " + PURGED_PHOTO + " END AS MBR_ATCH_FILE_SN")
                .contains("CASE WHEN C.DEL_YN = 'Y' THEN NULL ELSE " + PURGED_NCNM + " END AS MBR_NCNM");
    }

    @Test
    void 파기_판단_조건은_파기_코드가_쓰는_값과_같다() {
        // DB 없이 값을 확인할 수 없어, 조각의 조건이 MemberPurgeService 가 파기 때 넣는 값과 어긋나지 않는지 본다
        String prefix = (String) org.springframework.test.util.ReflectionTestUtils
                .getField(com.nyangmeong.member.service.MemberPurgeService.class, "PURGED_HMAC_PREFIX");
        assertThat(PURGED_NCNM).contains("M.MBR_STTS_CD = '"
                + com.nyangmeong.global.security.domain.MemberStatus.WITHDRAWN.getCode() + "'")
                .contains("M.ACNT_EML_HMAC LIKE '" + prefix + "%'");
    }

    @Test
    void 자동_건강팁도_작성자를_공통_조각으로_고른다() {
        Map<String, Object> param = new HashMap<>();
        param.put("keywords", java.util.List.of("건강"));
        String sql = configuration.getMappedStatement("com.nyangmeong.community.mapper.PostMapper.selectAutoHealthTips")
                .getBoundSql(param).getSql().replaceAll("\\s+", " ");
        assertThat(sql).contains(PURGED_PHOTO + " AS MBR_ATCH_FILE_SN").contains(PURGED_NCNM + " AS MBR_NCNM");
    }

    @Test
    void 관리자_게시글_목록은_원래_닉네임을_그대로_준다() {
        assertThat(sql("PostMapper.selectAdminList")).doesNotContain("탈퇴한 회원");
        assertThat(sql("PostMapper.selectMbrNcnm")).doesNotContain("탈퇴한 회원");
        assertThat(sql("CommentMapper.selectAdminList")).doesNotContain("탈퇴한 회원");
    }

    @Test
    void 실종_상세는_작성자_사진_번호를_고른다() {
        assertThat(sql("LostReportMapper.selectDetail")).contains("M.ATCH_FILE_SN AS MBR_ATCH_FILE_SN")
                .doesNotContain("MBR_STTS_CD");
    }
}
