package com.nyangmeong.health.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * [검사 - 관찰 매퍼 XML 조건]
 * - DB에 붙지 않고 SQL 문장만 확인 (docs/convention/test.md)
 * - 삭제 제외·회차 번호 매김·삭제는 DEL_YN 변경 (FN-HLT-002-15~19)
 */
class ObservationMapperXmlTest {

    private static final Path XML = Path.of("src/main/resources/mapper/health/ObservationMapper.xml");

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
    void 회차_번호는_삭제되지_않은_회차만_관찰일_순으로_매긴다() throws Exception {
        for (String id : new String[] { "selectCycles", "selectPetCycles" }) {
            String sql = sql("select", id);

            assertThat(sql)
                    .contains("ROW_NUMBER() OVER (PARTITION BY Y.OBSRV_ARTCL_SN ORDER BY Y.OBSRV_DT, Y.OBSRV_CYCL_SN)")
                    .contains("Y.DEL_YN = 'N'")
                    .contains("A.DEL_YN = 'N'");
        }
    }

    @Test
    void 회차_목록은_경로_반려동물의_항목만_읽는다() throws Exception {
        String sql = sql("select", "selectCycles");

        assertThat(sql).contains("A.PET_SN = #{cond.petSn}")
                .contains("A.OBSRV_ARTCL_SN = #{cond.obsrvArtclSn}")
                .contains("T.OBSRV_CYCL_SN = #{cond.obsrvCyclSn}")
                .contains("ORDER BY T.CYCL_NO");
    }

    @Test
    void 조회는_경로_반려동물의_삭제되지_않은_항목과_회차만_다룬다() throws Exception {
        assertThat(sql("select", "selectItems")).contains("A.PET_SN = #{cond.petSn}").contains("A.DEL_YN = 'N'");
        assertThat(sql("select", "selectItem")).contains("PET_SN = #{petSn}").contains("DEL_YN = 'N'");
        assertThat(sql("select", "selectCycle")).contains("A.PET_SN = #{petSn}")
                .contains("A.DEL_YN = 'N'")
                .contains("Y.DEL_YN = 'N'");
        assertThat(sql("select", "countCycles")).contains("DEL_YN = 'N'");
        assertThat(sql("select", "selectObservingPetSns")).contains("OBSRV_STTS_CD = #{obsrvSttsCd}")
                .contains("DEL_YN = 'N'");
    }

    @Test
    void 삭제는_DEL_YN을_바꾸는_UPDATE다() throws Exception {
        assertThat(sql("update", "deleteCycle")).contains("DEL_YN = 'Y'").contains("DEL_YN = 'N'");
        assertThat(sql("update", "deleteItem")).contains("DEL_YN = 'Y'").contains("DEL_YN = 'N'");
        assertThat(sql("update", "updateCycle")).contains("DEL_YN = 'N'");
    }

    @Test
    void 관찰_종료는_상태와_종료일을_바꾼다() throws Exception {
        assertThat(sql("update", "updateItemEnd")).contains("OBSRV_STTS_CD = #{obsrvSttsCd}")
                .contains("OBSRV_END_YMD = CURDATE()")
                .contains("DEL_YN = 'N'");
    }
}
