package com.nyangmeong.health.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * [검사 - 영수증 검진기록 매퍼 XML 조건]
 * - DB에 붙지 않고 SQL 문장만 확인 (docs/convention/test.md)
 * - 가까운 미연결 병원 일정(FN-HLT-002-46), 영수증으로 만든 검진기록 조회(FN-HLT-002-45)
 */
class CheckupReceiptMapperXmlTest {

    private static final Path XML = Path.of("src/main/resources/mapper/health/CheckupMapper.xml");

    private static String sql(String id) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Element mapper = factory.newDocumentBuilder().parse(XML.toFile()).getDocumentElement();
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
    void 가까운_일정은_같은_반려동물의_삭제되지_않은_병원_일정만_본다() throws Exception {
        String sql = sql("selectNearestUnlinkedHospitalSchedule");

        assertThat(sql).contains("S.PET_SN = #{petSn}")
                .contains("S.SCHDL_KND_CD = #{hospitalCd}")
                .contains("S.DEL_YN = 'N'");
    }

    @Test
    void 가까운_일정은_검진일_앞뒤_N일_경계를_포함한다() throws Exception {
        String sql = sql("selectNearestUnlinkedHospitalSchedule");

        assertThat(sql).contains("S.SCHDL_DT >= DATE_SUB(#{ymd}, INTERVAL #{days} DAY)")
                .contains("S.SCHDL_DT < DATE_ADD(#{ymd}, INTERVAL #{days} + 1 DAY)");
    }

    @Test
    void 가까운_일정은_완료_검진기록이_연결된_일정을_뺀다() throws Exception {
        String sql = sql("selectNearestUnlinkedHospitalSchedule");

        assertThat(sql).contains("NOT EXISTS ( SELECT 1 FROM CKUP_RCD C WHERE C.SCHDL_SN = S.SCHDL_SN")
                .contains("C.CKUP_RCD_STTS_CD = #{completedCd}")
                .contains("C.DEL_YN = 'N'");
    }

    @Test
    void 가까운_일정은_검진일과_가장_가까운_1건_같으면_먼저_등록된_일정이다() throws Exception {
        String sql = sql("selectNearestUnlinkedHospitalSchedule");

        assertThat(sql).contains("ORDER BY ABS(DATEDIFF(S.SCHDL_DT, #{ymd})) , S.SCHDL_SN LIMIT 1");
    }

    @Test
    void 영수증_검진기록_조회는_완료_상태와_삭제되지_않은_기록과_일정만_본다() throws Exception {
        String sql = sql("selectReceiptCheckups");

        assertThat(sql).contains("C.RECPT_SN = #{recptSn}")
                .contains("C.CKUP_RCD_STTS_CD = #{completedCd}")
                .contains("C.DEL_YN = 'N'")
                .contains("S.DEL_YN = 'N'")
                .contains("ORDER BY C.CKUP_RCD_SN");
    }
}
