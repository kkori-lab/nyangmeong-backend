package com.nyangmeong.health.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * [검사 - 메인 건강 지표 매퍼 XML 조건]
 * - DB에 붙지 않고 SQL 문장만 확인 (docs/convention/test.md)
 * - 날짜별 묶기·삭제 제외·기준일 이후 제외 (FN-MAN-006)
 */
class HealthIndicatorMapperXmlTest {

    private static final String DAILY = "src/main/resources/mapper/health/DailyHealthRecordMapper.xml";
    private static final String INSPECTION = "src/main/resources/mapper/health/InspectionValueMapper.xml";

    private static String sql(String xml, String id) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Element mapper = factory.newDocumentBuilder().parse(Path.of(xml).toFile()).getDocumentElement();
        NodeList nodes = mapper.getElementsByTagName("select");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            if (id.equals(node.getAttribute("id"))) {
                return node.getTextContent().replaceAll("\\s+", " ");
            }
        }
        throw new AssertionError("select 없음: " + id);
    }

    @Test
    void 체중_점은_삭제되지_않은_최근_N건을_오름차순으로_돌려준다() throws Exception {
        String sql = sql(DAILY, "selectWeightPoints");

        assertThat(sql).contains("R.PET_SN = #{petSn}")
                .contains("R.DAILY_RCD_IDCT_CD = #{weightCd}")
                .contains("R.DEL_YN = 'N'")
                .contains("ORDER BY R.RCD_DT DESC, R.DAILY_HLTH_RCD_SN DESC")
                .contains("LIMIT #{size}")
                .contains("ORDER BY T.RCD_DT, T.DAILY_HLTH_RCD_SN");
    }

    @Test
    void 일_합계_점은_날짜로_묶고_기준일_이후_기록과_삭제된_기록을_뺀다() throws Exception {
        String sql = sql(DAILY, "selectDailySumPoints");

        assertThat(sql).contains("SUM(R.RCD_NVL)")
                .contains("GROUP BY DATE(R.RCD_DT)")
                .contains("R.DEL_YN = 'N'")
                .contains("R.RCD_DT < DATE_ADD(#{baseYmd}, INTERVAL 1 DAY)")
                .contains("ORDER BY DATE(R.RCD_DT) DESC")
                .contains("LIMIT #{size}")
                .contains("ORDER BY T.RCD_YMD");
    }

    @Test
    void 크레아티닌_점은_수치_검진기록_일정의_삭제된_행을_모두_뺀다() throws Exception {
        String sql = sql(INSPECTION, "selectCreatininePoints");

        assertThat(sql).contains("V.DEL_YN = 'N'")
                .contains("C.DEL_YN = 'N'")
                .contains("S.DEL_YN = 'N'")
                .contains("S.PET_SN = #{petSn}")
                .contains("V.INSP_ARTCL_CD = #{creaCd}")
                .contains("V.INSP_ARTCL_DRCT_INPT_NM = ''")
                .contains("ORDER BY S.SCHDL_DT DESC")
                .contains("LIMIT #{size}")
                .contains("ORDER BY T.SCHDL_DT");
    }
}
