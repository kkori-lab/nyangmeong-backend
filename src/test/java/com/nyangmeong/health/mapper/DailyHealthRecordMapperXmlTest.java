package com.nyangmeong.health.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * [검사 - 일상 건강기록 매퍼 XML 조건]
 * - DB에 붙지 않고 SQL 문장만 확인 (docs/convention/test.md)
 * - 삭제 제외·기간 경계·최신 N건 오름차순 (FN-HLT-002-09·10·12·13)
 */
class DailyHealthRecordMapperXmlTest {

    private static final Path XML = Path.of("src/main/resources/mapper/health/DailyHealthRecordMapper.xml");

    private static String sql(String tag, String id) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Element mapper = factory.newDocumentBuilder().parse(XML.toFile()).getDocumentElement();
        NodeList nodes = mapper.getElementsByTagName(tag);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            if (id.equals(node.getAttribute("id"))) {
                return node.getTextContent().replaceAll("\\s+", " ");
            }
        }
        throw new AssertionError(tag + " 없음: " + id);
    }

    @Test
    void 목록은_삭제되지_않은_기록만_기간_경계로_거른다() throws Exception {
        String sql = sql("select", "selectList");

        assertThat(sql).contains("R.PET_SN = #{cond.petSn}")
                .contains("R.DEL_YN = 'N'")
                .contains("R.RCD_DT >= #{cond.fromYmd}")
                .contains("R.RCD_DT < DATE_ADD(#{cond.toYmd}, INTERVAL 1 DAY)")
                .contains("R.DAILY_RCD_IDCT_CD = #{cond.dailyRcdIdctCd}");
    }

    @Test
    void 목록은_최신_N건을_고른_뒤_기록_시각_오름차순으로_돌려준다() throws Exception {
        String sql = sql("select", "selectList");

        assertThat(sql).contains("ORDER BY R.RCD_DT DESC, R.DAILY_HLTH_RCD_SN DESC")
                .contains("LIMIT #{cond.size}")
                .contains("ORDER BY T.RCD_DT, T.DAILY_HLTH_RCD_SN");
    }

    @Test
    void 최신_체중은_삭제되지_않은_체중_기록_중_가장_늦은_1건이다() throws Exception {
        String sql = sql("select", "selectLatestWeight");

        assertThat(sql).contains("DEL_YN = 'N'")
                .contains("DAILY_RCD_IDCT_CD = #{weightCd}")
                .contains("ORDER BY RCD_DT DESC, DAILY_HLTH_RCD_SN DESC LIMIT 1");
    }

    @Test
    void 조회와_수정은_경로_반려동물의_삭제되지_않은_기록만_다루고_삭제는_DEL_YN을_바꾼다() throws Exception {
        assertThat(sql("select", "selectRecord")).contains("PET_SN = #{petSn}").contains("DEL_YN = 'N'");
        assertThat(sql("update", "deleteRecord")).contains("DEL_YN = 'Y'").contains("DEL_YN = 'N'");
        assertThat(sql("update", "updateRecord")).contains("DEL_YN = 'N'");
    }
}
