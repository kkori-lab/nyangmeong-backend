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
 * [검사 - 작성자 프로필 사진 SQL]
 * - DB 없이 Mapper XML 에서 만들어지는 SQL 에 작성자 사진 번호가 있고 회원 상태 조건은 없는지만 확인
 */
class PostMapperSqlTest {

    private static Configuration configuration;

    @BeforeAll
    static void loadMappers() throws Exception {
        configuration = new Configuration();
        for (String name : new String[] { "PostMapper", "CommentMapper", "LostReportMapper" }) {
            String path = "mapper/community/" + name + ".xml";
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
        assertThat(sql("PostMapper.selectList")).contains("M.ATCH_FILE_SN AS MBR_ATCH_FILE_SN")
                .doesNotContain("MBR_STTS_CD");
    }

    @Test
    void 오늘의_건강팁은_작성자_사진_번호를_고른다() {
        assertThat(sql("PostMapper.selectHealthTips")).contains("M.ATCH_FILE_SN AS MBR_ATCH_FILE_SN")
                .doesNotContain("MBR_STTS_CD");
    }

    @Test
    void 작성자_조회는_닉네임과_사진_번호를_함께_고른다() {
        assertThat(sql("PostMapper.selectAuthor")).contains("MBR_NCNM").contains("ATCH_FILE_SN AS mbrAtchFileSn")
                .doesNotContain("MBR_STTS_CD");
    }

    @Test
    void 댓글_목록은_삭제_댓글이면_사진_번호를_비운다() {
        assertThat(sql("CommentMapper.selectList"))
                .contains("CASE WHEN C.DEL_YN = 'Y' THEN NULL ELSE M.ATCH_FILE_SN END AS MBR_ATCH_FILE_SN")
                .doesNotContain("MBR_STTS_CD");
    }

    @Test
    void 실종_상세는_작성자_사진_번호를_고른다() {
        assertThat(sql("LostReportMapper.selectDetail")).contains("M.ATCH_FILE_SN AS MBR_ATCH_FILE_SN")
                .doesNotContain("MBR_STTS_CD");
    }
}
