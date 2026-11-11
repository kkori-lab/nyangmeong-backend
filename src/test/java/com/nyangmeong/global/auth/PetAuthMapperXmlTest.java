package com.nyangmeong.global.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * [검사 - 반려동물 권한 매퍼 XML 조건]
 * - DB에 붙지 않고 SQL 문장만 확인 (docs/convention/test.md)
 */
class PetAuthMapperXmlTest {

    private static final Path XML = Path.of("src/main/resources/mapper/global/PetAuthMapper.xml");

    private static String sql(String id) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Element mapper = factory.newDocumentBuilder().parse(XML.toFile()).getDocumentElement();
        NodeList selects = mapper.getElementsByTagName("select");
        for (int i = 0; i < selects.getLength(); i++) {
            Element select = (Element) selects.item(i);
            if (id.equals(select.getAttribute("id"))) {
                return select.getTextContent().replaceAll("\\s+", " ");
            }
        }
        throw new AssertionError("select 없음: " + id);
    }

    @Test
    void 양육_반려동물_목록은_역할_사용여부_삭제여부로_거른다() throws Exception {
        String sql = sql("selectCaregiverPetSns");

        assertThat(sql).contains("R.MBR_SN = #{mbrSn}")
                .contains("R.BRUP_ROLE_CD IN #{brupRoleCd}")
                .contains("R.USE_YN = 'Y'")
                .contains("P.DEL_YN = 'N'")
                .doesNotContain("PETC");
    }

    @Test
    void 번호를_주면_그_번호만_남긴다() throws Exception {
        String sql = sql("selectCaregiverPetSns");

        assertThat(sql).contains("R.PET_SN IN #{petSn}");
    }
}
