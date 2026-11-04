package com.nyangmeong.community.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.HashMap;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * [검사 - 펫돌봄 매칭 SQL]
 * - DB 없이 상태 변경이 현재 상태를 조건으로 거는지, 잠금 읽기인지만 확인
 */
class CareMatchMapperSqlTest {

    private static Configuration configuration;

    @BeforeAll
    static void loadMappers() throws Exception {
        configuration = new Configuration();
        for (String name : new String[] { "CareMatchMapper", "CareRecruitmentMapper" }) {
            String path = "mapper/community/" + name + ".xml";
            try (InputStream in = CareMatchMapperSqlTest.class.getResourceAsStream("/" + path)) {
                new XMLMapperBuilder(in, configuration, path, configuration.getSqlFragments()).parse();
            }
        }
    }

    private String sql(String statement) {
        return configuration.getMappedStatement("com.nyangmeong.community.mapper." + statement)
                .getBoundSql(new HashMap<String, Object>()).getSql().replaceAll("\\s+", " ");
    }

    @Test
    void 매칭_상태_변경은_현재_상태가_맞을_때만_바뀐다() {
        assertThat(sql("CareMatchMapper.updateStatus")).contains("SET MTCH_DLNG_STTS_CD = ?")
                .endsWith("AND MTCH_DLNG_STTS_CD = ?");
        assertThat(sql("CareMatchMapper.updateRequestAgain")).endsWith("AND MTCH_DLNG_STTS_CD = ?")
                .contains("RCV_YN = NULL");
    }

    @Test
    void 모집_상태_변경도_현재_상태를_조건으로_건다() {
        assertThat(sql("CareRecruitmentMapper.updateStatus")).contains("CFMTN_PET_PRTC_MTCH_SN = ?")
                .endsWith("AND RCRT_STTS_CD = ?");
    }

    @Test
    void 거래_단계_변경_전_읽기는_잠금_읽기다() {
        assertThat(sql("CareRecruitmentMapper.selectForUpdate")).endsWith("FOR UPDATE");
        assertThat(sql("CareMatchMapper.selectByIdForUpdate")).endsWith("FOR UPDATE");
        assertThat(sql("CareMatchMapper.selectByRoomForUpdate")).endsWith("FOR UPDATE");
        assertThat(sql("CareMatchMapper.selectByIdmptDmndIdForShare")).endsWith("FOR SHARE");
        assertThat(sql("CareMatchMapper.selectNextCndVerNo")).endsWith("FOR UPDATE");
        assertThat(sql("CareMatchMapper.selectNextDmndCycl")).endsWith("FOR UPDATE");
        assertThat(sql("CareMatchMapper.selectLatestCompletionRequestForUpdate")).endsWith("LIMIT 1 FOR UPDATE");
    }

    @Test
    void 일반_조회는_잠금을_걸지_않는다() {
        assertThat(sql("CareMatchMapper.selectById")).doesNotContain("FOR UPDATE");
        assertThat(sql("CareMatchMapper.selectLatestCondition")).doesNotContain("FOR UPDATE")
                .contains("ORDER BY V.CND_VER_NO DESC LIMIT 1");
        assertThat(sql("CareMatchMapper.selectCompletionRequests")).contains("ORDER BY D.DMND_CYCL");
    }

    @Test
    void 수령_확인은_수령_여부와_시각을_함께_기록한다() {
        assertThat(sql("CareMatchMapper.updateReceipt")).contains("RCV_YN = ?").contains("RCV_IDNTY_DT = NOW()");
    }
}
