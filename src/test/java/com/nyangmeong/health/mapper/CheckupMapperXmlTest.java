package com.nyangmeong.health.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * [검사 - 검진기록 매퍼 XML 조건]
 * - DB에 붙지 않고 SQL 문장만 확인 (docs/convention/test.md)
 * - 연도·종류 필터·삭제 상태 제외·삭제된 일정 표시·병원 일정만 (FN-HLT-002-05·06·14)
 */
class CheckupMapperXmlTest {

    private static final Path XML = Path.of("src/main/resources/mapper/health/CheckupMapper.xml");

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
    void 목록은_완료_상태의_병원_일정_검진기록만_반려동물로_거른다() throws Exception {
        String sql = sql("select", "selectList");

        assertThat(sql).contains("S.PET_SN = #{petSn}")
                .contains("S.SCHDL_KND_CD = #{hospitalCd}")
                .contains("C.CKUP_RCD_STTS_CD = #{completedCd}");
    }

    @Test
    void 목록은_조회_연도와_검진유형이_있을_때만_거른다() throws Exception {
        String sql = sql("select", "selectList");

        assertThat(sql).contains("S.SCHDL_DT >= MAKEDATE(#{cond.year}, 1)")
                .contains("S.SCHDL_DT < DATE_ADD(MAKEDATE(#{cond.year}, 1), INTERVAL 1 YEAR)")
                .contains("C.CKUP_TYPE_CD = #{cond.ckupTypeCd}");
    }

    @Test
    void 목록은_삭제된_일정도_빼지_않고_삭제_여부를_돌려준다() throws Exception {
        String sql = sql("select", "selectList");

        assertThat(sql).doesNotContain("S.DEL_YN = 'N'")
                .contains("S.DEL_YN = 'Y'")
                .contains("AS SCHDL_DEL_YN");
    }

    @Test
    void 목록은_검진일_최신순_같으면_번호_큰_순으로_정렬하고_LIMIT을_직접_쓰지_않는다() throws Exception {
        String sql = sql("select", "selectList");

        assertThat(sql).contains("ORDER BY S.SCHDL_DT DESC , C.CKUP_RCD_SN DESC")
                .doesNotContain("LIMIT");
    }

    @Test
    void 단건_조회는_완료_상태만_보고_반려동물은_일정에서_가져온다() throws Exception {
        String sql = sql("select", "selectDetail");

        assertThat(sql).contains("C.CKUP_RCD_SN = #{ckupRcdSn}")
                .contains("C.CKUP_RCD_STTS_CD = #{completedCd}")
                .contains("S.PET_SN");
    }

    @Test
    void 같은_날짜_병원_일정은_삭제되지_않은_것_중_먼저_등록된_하나를_고른다() throws Exception {
        String sql = sql("select", "selectHospitalScheduleSn");

        assertThat(sql).contains("DEL_YN = 'N'")
                .contains("SCHDL_KND_CD = #{hospitalCd}")
                .contains("SCHDL_DT >= #{ymd}")
                .contains("SCHDL_DT < DATE_ADD(#{ymd}, INTERVAL 1 DAY)")
                .contains("ORDER BY SCHDL_SN LIMIT 1");
    }

    @Test
    void 연결_여부는_완료_상태_검진기록만_센다() throws Exception {
        assertThat(sql("select", "existsLinkedCheckup")).contains("SCHDL_SN = #{schdlSn}")
                .contains("CKUP_RCD_STTS_CD = #{completedCd}");
    }

    @Test
    void 삭제는_케어_항목과_반복_예방을_DEL_YN으로_지우고_완료_기록은_건드리지_않는다() throws Exception {
        String careItems = sql("update", "deleteLinkedCareItems");
        String repeat = sql("update", "deleteLinkedRepeatPreventions");

        assertThat(careItems).contains("SET DEL_YN = 'Y'")
                .contains("P.CKUP_RCD_SN = #{ckupRcdSn}")
                .contains("R.CKUP_RCD_SN = #{ckupRcdSn}")
                .doesNotContain("CARE_CMPTN_HSTRY");
        assertThat(repeat).contains("SET DEL_YN = 'Y'").contains("CKUP_RCD_SN = #{ckupRcdSn}");
    }

    @Test
    void 반려동물_이동은_케어_항목의_메인_표시를_해제한다() throws Exception {
        assertThat(sql("update", "updateLinkedCareItemsPet")).contains("PET_SN = #{petSn}")
                .contains("MAIN_EXPSR_YN = 'N'");
    }
}
