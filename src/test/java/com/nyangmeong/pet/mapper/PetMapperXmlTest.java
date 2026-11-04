package com.nyangmeong.pet.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * [검사 - 반려동물 삭제 시 공동양육 자동 해제 매퍼 XML]
 * - DB에 붙지 않고 SQL 문장만 확인 (docs/convention/test.md)
 * - 연동 중인 공동양육 관계만 고르는 조회, 연동 중인 행만 해제하는 갱신 (FN-MYP-002-08)
 */
class PetMapperXmlTest {

    private static String sql(String tag, String id) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Path xml = Path.of("src/main/resources/mapper/pet/PetMapper.xml");
        Element root = factory.newDocumentBuilder().parse(xml.toFile()).getDocumentElement();
        NodeList nodes = root.getElementsByTagName(tag);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            if (id.equals(node.getAttribute("id"))) {
                return node.getTextContent().replaceAll("\\s+", " ");
            }
        }
        throw new AssertionError(tag + " 없음: PetMapper." + id);
    }

    @Test
    void 연동_중인_공동양육자만_고른다() throws Exception {
        assertThat(sql("select", "selectLinkedCoParentRels"))
                .contains("FROM BRUP_REL")
                .contains("WHERE PET_SN = #{petSn}")
                .contains("AND BRUP_ROLE_CD = #{coParentRoleCd}")
                .contains("AND USE_YN = 'Y'");
    }

    @Test
    void 체중_수정은_PET_BDWG만_바꾸고_삭제되지_않은_행만_고른다() throws Exception {
        String sql = sql("update", "updatePetWeight");
        assertThat(sql)
                .contains("UPDATE PET")
                .contains("SET PET_BDWG = #{weight}")
                .contains("MDFR_ID = #{actorId}")
                .contains("MDFCN_DT = NOW()")
                .contains("WHERE PET_SN = #{petSn}")
                .contains("AND DEL_YN = 'N'")
                .doesNotContain("GOAL_BDWG")
                .doesNotContain("MIN_BDWG")
                .doesNotContain("MAX_BDWG");
    }

    @Test
    void 연동_중인_행만_해제하고_대표_표시도_끈다() throws Exception {
        assertThat(sql("update", "updateBrupRelUnlink"))
                .contains("UPDATE BRUP_REL")
                .contains("SET USE_YN = 'N'")
                .contains(", RPRS_PET_YN = 'N'")
                .contains("MDFR_ID = #{actorId}")
                .contains("MDFCN_DT = NOW()")
                .contains("WHERE BRUP_REL_SN = #{brupRelSn} AND USE_YN = 'Y'");
    }
}
