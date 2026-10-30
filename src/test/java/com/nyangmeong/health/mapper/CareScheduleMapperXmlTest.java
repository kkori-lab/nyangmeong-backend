package com.nyangmeong.health.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * [검사 - 일정 매퍼 XML 조건]
 * - DB에 붙지 않고 SQL 문장만 확인 (docs/convention/test.md)
 * - 기간·삭제 제외·정렬·건수 제한 (FN-HLT-003-29·31·33·40)
 */
class CareScheduleMapperXmlTest {

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
    void 목록은_반려동물과_삭제되지_않은_일정만_기간으로_거른다() throws Exception {
        String sql = sql("selectList");

        assertThat(sql).contains("S.PET_SN = #{petSn}")
                .contains("S.DEL_YN = 'N'")
                .contains("S.SCHDL_DT >= #{cond.fromYmd}")
                .contains("S.SCHDL_DT < DATE_ADD(#{cond.toYmd}, INTERVAL 1 DAY)");
    }

    @Test
    void 목록은_날짜_시간입력_시각_등록순으로_정렬하고_건수를_제한한다() throws Exception {
        String sql = sql("selectList");

        assertThat(sql).contains("ORDER BY DATE(S.SCHDL_DT) , S.HR_INPT_YN DESC , S.SCHDL_DT , S.SCHDL_SN")
                .contains("LIMIT #{cond.size}");
        assertThat(sql.indexOf("ORDER BY")).isLessThan(sql.indexOf("LIMIT"));
    }

    @Test
    void 목록은_연결_검진기록_수와_코드명을_함께_준다() throws Exception {
        String sql = sql("selectList");

        assertThat(sql).contains("C.SCHDL_SN = S.SCHDL_SN")
                .contains("AS CKUP_RCD_CNT")
                .contains("AS SCHDL_KND_CD_NM")
                .contains("AS CKUP_TYPE_CD_NM");
    }

    @Test
    void 연결_검진기록_수는_삭제_상태_검진기록을_세지_않는다() throws Exception {
        String sql = sql("selectList");

        assertThat(sql).contains("C.SCHDL_SN = S.SCHDL_SN AND C.CKUP_RCD_STTS_CD = #{completedCd}");
    }

    @Test
    void 단건_조회는_삭제된_일정을_빼서_삭제_뒤_다시_찾지_못한다() throws Exception {
        assertThat(sql("selectSchedule")).contains("DEL_YN = 'N'");
    }

    @Test
    void 병원_일정_알림_대상은_삭제된_일정과_삭제된_반려동물을_뺀다() throws Exception {
        String sql = sql("selectHospitalSchedules");

        assertThat(sql).contains("JOIN PET P ON P.PET_SN = S.PET_SN AND P.DEL_YN = 'N'")
                .contains("S.DEL_YN = 'N'")
                .contains("S.SCHDL_KND_CD = #{hospitalCd}")
                .doesNotContain("S.PET_SN = #{petSn}");
    }

    @Test
    void 병원_일정_알림_대상은_일정일_하루만_고른다() throws Exception {
        String sql = sql("selectHospitalSchedules");

        assertThat(sql).contains("S.SCHDL_DT >= #{schdlYmd}")
                .contains("S.SCHDL_DT < DATE_ADD(#{schdlYmd}, INTERVAL 1 DAY)")
                .contains("ORDER BY S.SCHDL_DT , S.SCHDL_SN");
    }

    @Test
    void 연결_검진기록_수는_삭제된_검진기록을_세지_않는다() throws Exception {
        assertThat(sql("selectList")).contains("AND C.DEL_YN = 'N' ) AS CKUP_RCD_CNT");
    }
}
