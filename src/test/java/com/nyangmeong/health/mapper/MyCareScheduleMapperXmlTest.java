package com.nyangmeong.health.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * [검사 - 여러 반려동물 일정 매퍼 XML 조건]
 * - DB에 붙지 않고 SQL 문장만 확인 (docs/convention/test.md)
 * - 반려동물 목록·삭제 제외·반려동물마다 건수 제한·정렬 (FN-HLT-003-29·31·40)
 */
class MyCareScheduleMapperXmlTest {

    private static final Path XML = Path.of("src/main/resources/mapper/health/CareScheduleMapper.xml");

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
    void 여러_반려동물_목록은_반려동물_목록과_삭제되지_않은_일정만_기간으로_거른다() throws Exception {
        String sql = sql("selectListByPets");

        assertThat(sql).contains("S.PET_SN IN #{petSn}")
                .doesNotContain("S.PET_SN = #{petSn}")
                .contains("S.DEL_YN = 'N'")
                .contains("S.SCHDL_DT >= #{cond.fromYmd}")
                .contains("S.SCHDL_DT < DATE_ADD(#{cond.toYmd}, INTERVAL 1 DAY)")
                .contains("S.SCHDL_KND_CD = #{cond.schdlKndCd}")
                .contains("S.CKUP_TYPE_CD = #{cond.ckupTypeCd}");
    }

    @Test
    void 건수는_반려동물마다_순번으로_자른다() throws Exception {
        String sql = sql("selectListByPets");

        assertThat(sql).contains("ROW_NUMBER() OVER (PARTITION BY S.PET_SN ORDER BY DATE(S.SCHDL_DT) ,"
                + " S.HR_INPT_YN DESC , S.SCHDL_DT , S.SCHDL_SN) AS PET_SCHDL_SEQ")
                .contains("L.PET_SCHDL_SEQ <= #{cond.size}")
                .doesNotContain("LIMIT");
    }

    @Test
    void 최종_정렬은_한_마리_목록과_같다() throws Exception {
        String sql = sql("selectListByPets");

        assertThat(sql).contains("S.HR_INPT_YN AS SORT_HR_INPT_YN")
                .endsWith("ORDER BY DATE(L.SCHDL_DT) , L.SORT_HR_INPT_YN DESC , L.SCHDL_DT , L.SCHDL_SN ");
        assertThat(sql("selectList")).contains("ORDER BY DATE(S.SCHDL_DT) , S.HR_INPT_YN DESC , S.SCHDL_DT , S.SCHDL_SN");
    }

    @Test
    void 한_마리_목록과_같은_컬럼과_검진기록_수를_준다() throws Exception {
        String sql = sql("selectListByPets");

        assertThat(sql).contains("C.SCHDL_SN = S.SCHDL_SN AND C.CKUP_RCD_STTS_CD = #{completedCd} AND C.DEL_YN = 'N'")
                .contains("K.COM_CD_NM AS SCHDL_KND_CD_NM")
                .contains("T.COM_CD_NM AS CKUP_TYPE_CD_NM")
                .contains("CASE WHEN S.HR_INPT_YN = 'Y' THEN TRUE ELSE FALSE END AS HR_INPT_YN")
                .contains("L.CLDR_BADGE_COLR_CD , L.CKUP_RCD_CNT FROM (");
    }
}
