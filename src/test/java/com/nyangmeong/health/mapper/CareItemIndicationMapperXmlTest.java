package com.nyangmeong.health.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * [검사 - 케어리스트 표시 여부·완료 시각 SQL] (※신규 정책)
 * - DB에 붙지 않고 SQL 문장만 확인 (docs/convention/test.md)
 */
class CareItemIndicationMapperXmlTest {

    private static final Path DIR = Path.of("src/main/resources/mapper/health");

    private static String sql(String file, String tag, String id) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Element mapper = factory.newDocumentBuilder().parse(DIR.resolve(file).toFile()).getDocumentElement();
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
    void 목록_행의_표시여부는_연동_항목이면_항상_보이고_고른_적_없는_기본_항목만_NULL이다() throws Exception {
        assertThat(sql("CareItemMapper.xml", "sql", "careItemSummaryColumns"))
                .contains("CASE WHEN A.PRSC_SN IS NOT NULL OR A.REPT_PRVNT_MNG_SN IS NOT NULL THEN TRUE")
                .contains("WHEN A.INDCT_YN = 'Y' THEN TRUE")
                .contains("WHEN A.INDCT_YN = 'N' THEN FALSE")
                .contains("WHEN A.BSC_ARTCL_YN = 'Y' THEN NULL")
                .contains("ELSE TRUE END AS INDCT_YN");
    }

    @Test
    void 표시_저장은_반려동물의_삭제되지_않은_항목_전부를_Y_N으로_바꾼다() throws Exception {
        assertThat(sql("CareItemMapper.xml", "update", "updateIndications"))
                .contains("SET INDCT_YN = CASE WHEN CARE_ARTCL_SN IN")
                .contains("THEN 'Y' ELSE 'N' END")
                .contains("WHERE PET_SN = #{petSn}")
                .contains("AND DEL_YN = 'N'");
    }

    @Test
    void 완료_기록의_완료_시각은_마지막으로_체크한_수정일시다() throws Exception {
        assertThat(sql("CareItemMapper.xml", "select", "selectCompletionList")).contains("H.MDFCN_DT AS CMPTN_DT");
    }

    @Test
    void 완료_기록_목록은_항목_표시여부로_거르지_않는다() throws Exception {
        assertThat(sql("CareItemMapper.xml", "select", "selectCompletionList")).doesNotContain("INDCT_YN");
    }

    @Test
    void 일정의_반려동물을_옮기는_SQL은_표시여부를_비운다() throws Exception {
        assertThat(sql("CareScheduleMapper.xml", "update", "updateLinkedCareItemsPet")).contains("INDCT_YN = NULL");
    }
}
