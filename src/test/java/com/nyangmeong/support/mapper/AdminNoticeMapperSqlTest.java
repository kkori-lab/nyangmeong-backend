package com.nyangmeong.support.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.nyangmeong.support.domain.Notice;

/**
 * [검사 - 관리자 공지 SQL]
 * - DB 없이 Mapper XML 에서 만들어지는 SQL 의 조건·정렬만 확인
 */
class AdminNoticeMapperSqlTest {

    private static final String NS = "com.nyangmeong.support.mapper.NoticeMapper.";

    private static Configuration configuration;

    @BeforeAll
    static void loadMapper() throws Exception {
        configuration = new Configuration();
        try (InputStream in = AdminNoticeMapperSqlTest.class.getResourceAsStream("/mapper/support/NoticeMapper.xml")) {
            new XMLMapperBuilder(in, configuration, "mapper/support/NoticeMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
    }

    private String sql(String id, Object param) {
        return configuration.getMappedStatement(NS + id).getBoundSql(param).getSql().replaceAll("\\s+", " ");
    }

    private String adminListSql(String keyword) {
        Map<String, Object> param = new HashMap<>();
        param.put("keyword", keyword);
        return sql("selectAdminList", param);
    }

    @Test
    void 관리자_목록은_비노출도_포함하고_고정_먼저_최신순이다() {
        String sql = adminListSql(null);

        assertThat(sql).doesNotContain("WHERE");
        assertThat(sql).contains("ORDER BY N.UPEND_FIX_YN DESC, N.REG_DT DESC, N.NTC_MTTR_SN DESC");
        assertThat(sql).contains("N.NOTI_SNDNG_DT").contains("N.NOTI_TRGT_CNT");
    }

    @Test
    void 관리자_목록_검색은_제목만_본다() {
        String sql = adminListSql("점검");

        assertThat(sql).contains("WHERE N.NTC_MTTR_TTL LIKE CONCAT('%', ?, '%')");
        assertThat(sql).doesNotContain("NTC_MTTR_CN LIKE");
    }

    @Test
    void 관리자_단건은_사용_여부를_보지_않는다() {
        assertThat(sql("selectAdminNotice", 5L)).doesNotContain("USE_YN =");
    }

    @Test
    void 수정은_보낸_값만_바꾼다() {
        Notice notice = new Notice();
        notice.setNtcMttrSn(5L);
        notice.setUseYn("N");
        notice.setMdfrId("9");

        String sql = sql("update", notice);

        assertThat(sql).contains("USE_YN = ?");
        assertThat(sql).doesNotContain("NTC_MTTR_TTL = ?").doesNotContain("UPEND_FIX_YN = ?");
    }

    @Test
    void 발송_기록은_시각과_대상_수를_남긴다() {
        Map<String, Object> param = new HashMap<>();
        param.put("ntcMttrSn", 5L);
        param.put("notiTrgtCnt", 3);

        assertThat(sql("updateNotificationSent", param)).contains("NOTI_SNDNG_DT = NOW()")
                .contains("NOTI_TRGT_CNT = ?");
    }
}
