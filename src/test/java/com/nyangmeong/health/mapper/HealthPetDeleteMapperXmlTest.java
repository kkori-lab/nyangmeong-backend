package com.nyangmeong.health.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * [검사 - 반려동물 삭제 시 건강기록 삭제 매퍼 XML]
 * - DB에 붙지 않고 SQL 문장만 확인 (docs/convention/test.md)
 * - 반려동물 번호로 찾는 경로, 대상 테이블만 DEL_YN = 'N' → 'Y', 연결 일정 삭제 여부는 보지 않음 (FN-MYP-002-07)
 */
class HealthPetDeleteMapperXmlTest {

    private static String sql(String mapper, String id) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Path xml = Path.of("src/main/resources/mapper/health/" + mapper + ".xml");
        Element root = factory.newDocumentBuilder().parse(xml.toFile()).getDocumentElement();
        NodeList updates = root.getElementsByTagName("update");
        for (int i = 0; i < updates.getLength(); i++) {
            Element update = (Element) updates.item(i);
            if (id.equals(update.getAttribute("id"))) {
                return update.getTextContent().replaceAll("\\s+", " ");
            }
        }
        throw new AssertionError("update 없음: " + mapper + "." + id);
    }

    @Test
    void 반려동물_번호를_가진_테이블은_반려동물_번호로_지운다() throws Exception {
        String[][] targets = {
                { "CareItemMapper", "CARE_ARTCL" },
                { "RepeatPreventionMapper", "REPT_PRVNT_MNG" },
                { "CareScheduleMapper", "SCHDL" },
                { "ObservationMapper", "OBSRV_ARTCL" },
                { "DailyHealthRecordMapper", "DAILY_HLTH_RCD" } };
        for (String[] target : targets) {
            assertThat(sql(target[0], "deleteByPetSn")).as(target[0])
                    .contains("UPDATE " + target[1] + " SET DEL_YN = 'Y'")
                    .contains("MDFR_ID = #{actorId}")
                    .contains("MDFCN_DT = NOW()")
                    .contains("WHERE PET_SN = #{petSn} AND DEL_YN = 'N'");
        }
    }

    @Test
    void 검진기록_자식은_검진기록과_일정을_거쳐_반려동물로_찾고_일정_삭제_여부는_보지_않는다() throws Exception {
        String[][] targets = {
                { "PrescriptionMapper", "PRSC", "R" },
                { "InspectionValueMapper", "INSP_NVL", "V" },
                { "EtcInspectionMapper", "ETC_INSP", "E" },
                { "CheckupAttachmentMapper", "CKUP_RCD_ATCH", "A" } };
        for (String[] target : targets) {
            String alias = target[2];
            assertThat(sql(target[0], "deleteByPetSn")).as(target[0])
                    .contains("UPDATE " + target[1] + " " + alias)
                    .contains("JOIN CKUP_RCD C ON C.CKUP_RCD_SN = " + alias + ".CKUP_RCD_SN")
                    .contains("JOIN SCHDL S ON S.SCHDL_SN = C.SCHDL_SN")
                    .contains("SET " + alias + ".DEL_YN = 'Y'")
                    .contains(alias + ".MDFR_ID = #{actorId}")
                    .contains("WHERE S.PET_SN = #{petSn} AND " + alias + ".DEL_YN = 'N'")
                    .doesNotContain("S.DEL_YN");
        }
    }

    @Test
    void 검진기록은_일정을_거쳐_반려동물로_찾고_일정_삭제_여부는_보지_않는다() throws Exception {
        assertThat(sql("CheckupMapper", "deleteByPetSn"))
                .contains("UPDATE CKUP_RCD C JOIN SCHDL S ON S.SCHDL_SN = C.SCHDL_SN")
                .contains("SET C.DEL_YN = 'Y'")
                .contains("WHERE S.PET_SN = #{petSn} AND C.DEL_YN = 'N'")
                .doesNotContain("S.DEL_YN");
    }

    @Test
    void 케어_완료_기록과_관찰_회차는_부모_항목의_반려동물로_찾는다() throws Exception {
        assertThat(sql("CareItemMapper", "deleteCompletionsByPetSn"))
                .contains("UPDATE CARE_CMPTN_HSTRY H JOIN CARE_ARTCL A ON A.CARE_ARTCL_SN = H.CARE_ARTCL_SN")
                .contains("WHERE A.PET_SN = #{petSn} AND H.DEL_YN = 'N'")
                .doesNotContain("A.DEL_YN");
        assertThat(sql("ObservationMapper", "deleteCyclesByPetSn"))
                .contains("UPDATE OBSRV_CYCL Y JOIN OBSRV_ARTCL A ON A.OBSRV_ARTCL_SN = Y.OBSRV_ARTCL_SN")
                .contains("WHERE A.PET_SN = #{petSn} AND Y.DEL_YN = 'N'")
                .doesNotContain("A.DEL_YN");
    }

    @Test
    void 케어_항목_삭제는_메인_표시도_해제한다() throws Exception {
        assertThat(sql("CareItemMapper", "deleteByPetSn")).contains("MAIN_EXPSR_YN = 'N'")
                .contains("MAIN_SORT_SEQ = 0");
    }
}
