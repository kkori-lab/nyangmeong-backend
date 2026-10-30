package com.nyangmeong.health.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * [검사 - 반려동물 전체 조회 매퍼 XML 조건]
 * - DB에 붙지 않고 SQL 문장만 확인 (docs/convention/test.md)
 * - 검진기록 번호 목록(IN)·삭제 제외·정렬 (FN-HLT-011-03·06~09)
 */
class HealthOverviewMapperXmlTest {

    private static String sql(String file, String id) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Element mapper = factory.newDocumentBuilder()
                .parse(Path.of("src/main/resources/mapper/health/" + file).toFile()).getDocumentElement();
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
    void 검사_수치는_검진기록_목록으로_거르고_삭제를_뺀다() throws Exception {
        String sql = sql("InspectionValueMapper.xml", "selectListByCheckups");

        assertThat(sql).contains("V.CKUP_RCD_SN IN").contains("V.DEL_YN = 'N'")
                .contains("ORDER BY V.CKUP_RCD_SN , V.SORT_SEQ , V.INSP_NVL_SN");
    }

    @Test
    void 처방약은_검진기록_목록으로_거르고_복용중_우선_최신순이다() throws Exception {
        String sql = sql("PrescriptionMapper.xml", "selectListByCheckups");

        assertThat(sql).contains("P.CKUP_RCD_SN IN").contains("P.DEL_YN = 'N'")
                .contains("ORDER BY CASE WHEN P.PRSC_STTS_CD = #{takingCd} THEN 0 ELSE 1 END , P.MDCN_BGNG_YMD DESC , P.PRSC_SN DESC");
    }

    @Test
    void 첨부는_검진기록_목록으로_거르고_최신순이다() throws Exception {
        String sql = sql("CheckupAttachmentMapper.xml", "selectListByCheckups");

        assertThat(sql).contains("A.CKUP_RCD_SN IN")
                .contains("A.DEL_YN = 'N'").contains("ORDER BY A.REG_DT DESC , A.CKUP_RCD_ATCH_SN DESC");
    }

    @Test
    void 반복_예방은_검진기록_목록으로_거르고_삭제를_뺀다() throws Exception {
        String sql = sql("RepeatPreventionMapper.xml", "selectListByCheckups");

        assertThat(sql).contains("R.CKUP_RCD_SN IN").contains("R.DEL_YN = 'N'").contains("C.DEL_YN = 'N'");
    }
}
