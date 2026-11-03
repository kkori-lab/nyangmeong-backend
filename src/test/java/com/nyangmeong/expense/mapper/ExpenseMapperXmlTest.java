package com.nyangmeong.expense.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * [검사 - 지출 조건 삭제 매퍼 XML]
 * - DB에 붙지 않고 SQL 문장만 확인 (docs/convention/test.md)
 * - 제외 번호를 빼고 지우는 갱신, 제외 번호 건수 조회 (FN-MYP-030-18·19·21)
 */
class ExpenseMapperXmlTest {

    private static String sql(String tag, String id) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Path xml = Path.of("src/main/resources/mapper/expense/ExpenseMapper.xml");
        Element root = factory.newDocumentBuilder().parse(xml.toFile()).getDocumentElement();
        NodeList nodes = root.getElementsByTagName(tag);
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            if (id.equals(node.getAttribute("id"))) {
                return node.getTextContent().replaceAll("\\s+", " ");
            }
        }
        throw new AssertionError(tag + " 없음: ExpenseMapper." + id);
    }

    @Test
    void 조건_삭제는_삭제_안_된_내_지출에서_제외_번호를_뺀다() throws Exception {
        assertThat(sql("update", "deleteByCond"))
                .contains("SET E.DEL_YN = 'Y'")
                .contains("WHERE E.MBR_SN = #{mbrSn} AND E.DEL_YN = 'N'")
                .contains("AND E.EXPND_SN NOT IN")
                .contains("#{expndSn}");
    }

    @Test
    void 제외_번호_건수는_삭제_안_된_내_지출_중_번호_목록에_든_것만_센다() throws Exception {
        assertThat(sql("select", "selectCountIn"))
                .contains("SELECT COUNT(*) FROM EXPND E")
                .contains("WHERE E.MBR_SN = #{mbrSn} AND E.DEL_YN = 'N'")
                .contains("AND E.EXPND_SN IN")
                .contains("#{expndSn}");
    }
}
