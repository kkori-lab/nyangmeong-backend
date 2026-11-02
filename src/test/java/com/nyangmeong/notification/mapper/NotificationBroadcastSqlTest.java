package com.nyangmeong.notification.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * [검사 - 공지 알림 일괄 등록 SQL]
 * - DB 없이 Mapper XML 에서 만들어지는 SQL 의 대상 조건만 확인
 */
class NotificationBroadcastSqlTest {

    private static Configuration configuration;

    @BeforeAll
    static void loadMapper() throws Exception {
        configuration = new Configuration();
        try (InputStream in = NotificationBroadcastSqlTest.class
                .getResourceAsStream("/mapper/notification/NotificationMapper.xml")) {
            new XMLMapperBuilder(in, configuration, "mapper/notification/NotificationMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
    }

    @Test
    void 정상_회원_중_앱_알림을_켠_회원에게만_한_번에_넣는다() {
        Map<String, Object> param = new HashMap<>();
        param.put("mbrSttsCd", "USRC0001");
        param.put("notiTypeCd", "AIMC0015");
        param.put("notiTtl", "새 공지사항이 있어요");
        param.put("notiCn", "점검 안내");
        param.put("mvmnPath", "/notices/7");

        String sql = configuration
                .getMappedStatement("com.nyangmeong.notification.mapper.NotificationMapper.insertForNormalMembers")
                .getBoundSql(param).getSql().replaceAll("\\s+", " ");

        assertThat(sql).contains("INSERT INTO INAPP_NOTI").contains("SELECT M.MBR_SN");
        assertThat(sql).contains("WHERE M.MBR_STTS_CD = ?").contains("AND M.INAPP_NOTI_RCPTN_YN = 'Y'");
        assertThat(sql).doesNotContain("VALUES");
    }
}
