package com.nyangmeong.health.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * [검사 - 케어 항목 매퍼 XML 조건]
 * - DB에 붙지 않고 SQL 문장만 확인 (docs/convention/test.md)
 * - 삭제 제외·정렬·되살림·잠금 읽기 (FN-HLT-003-12·13)
 */
class CareItemMapperXmlTest {

    private static final Path XML = Path.of("src/main/resources/mapper/health/CareItemMapper.xml");

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
    void 목록은_반려동물의_삭제되지_않은_항목만_정기관리를_먼저_돌려준다() throws Exception {
        String sql = sql("select", "selectList");

        assertThat(sql).contains("A.PET_SN = #{petSn}")
                .contains("A.DEL_YN = 'N'")
                .contains("ORDER BY CASE WHEN A.CARE_ZONE_CD = #{regularCd} THEN 0 ELSE 1 END , A.CARE_ARTCL_SN");
    }

    @Test
    void 메인_표시_조회는_메인_정렬_순서로_돌려준다() throws Exception {
        assertThat(sql("select", "selectList")).contains("A.MAIN_EXPSR_YN = 'Y'")
                .contains("ORDER BY A.MAIN_SORT_SEQ , A.CARE_ARTCL_SN");
    }

    @Test
    void 같은_이름_확인은_항목명이_같은_항목만_거른다() throws Exception {
        assertThat(sql("select", "selectList")).contains("A.CARE_ARTCL_NM = #{cond.careArtclNm}");
    }

    @Test
    void 목록의_처방약과_반복_예방_조인은_삭제된_행을_뺀다() throws Exception {
        String sql = sql("select", "selectList");

        assertThat(sql).contains("P.PRSC_SN = A.PRSC_SN AND P.DEL_YN = 'N'")
                .contains("R.REPT_PRVNT_MNG_SN = A.REPT_PRVNT_MNG_SN AND R.DEL_YN = 'N'");
    }

    @Test
    void 오늘_완료_건수는_삭제되지_않은_오늘_기록의_회차_합이다() throws Exception {
        String columns = sql("sql", "careItemSummaryColumns");

        assertThat(columns).contains("SUM(H.CMPTN_CYCL)")
                .contains("H.CMPTN_YMD = #{today}")
                .contains("H.DEL_YN = 'N'");
    }

    @Test
    void 항목_단건과_잠금_조회는_삭제된_항목을_빼서_삭제_뒤_다시_찾지_못한다() throws Exception {
        assertThat(sql("select", "selectSummary")).contains("A.DEL_YN = 'N'");
        assertThat(sql("select", "selectItemForUpdate")).contains("DEL_YN = 'N'").contains("FOR UPDATE");
    }

    @Test
    void 항목_삭제는_DELETE가_아니라_DEL_YN_UPDATE이고_메인_표시도_푼다() throws Exception {
        String sql = sql("update", "deleteItem");

        assertThat(sql).contains("SET DEL_YN = 'Y'").contains("MAIN_EXPSR_YN = 'N'");
    }

    @Test
    void 기록_내용은_삭제되지_않은_완료_기록을_최근_날짜부터_돌려준다() throws Exception {
        String sql = sql("select", "selectCompletionList");

        assertThat(sql).contains("H.DEL_YN = 'N'")
                .contains("A.DEL_YN = 'N'")
                .contains("ORDER BY H.CMPTN_YMD DESC , H.CARE_CMPTN_HSTRY_SN DESC");
    }

    @Test
    void 기록_내용은_투약_항목을_1일_복용_횟수를_채운_날만_돌려준다() throws Exception {
        assertThat(sql("select", "selectCompletionList"))
                .contains("H.CMPTN_CYCL >= COALESCE(P.DAY_MDCN_NMTM, 1)");
    }

    @Test
    void 완료일_조회는_삭제된_기록까지_잠금_읽기로_가져와_되살림을_가린다() throws Exception {
        String sql = sql("select", "selectCompletionByDateForUpdate");

        assertThat(sql).contains("CMPTN_YMD = #{cmptnYmd}").contains("FOR UPDATE").doesNotContain("DEL_YN = 'N'");
    }

    @Test
    void 완료_기록_등록은_삭제된_같은_날짜_기록을_되살린다() throws Exception {
        String sql = sql("insert", "upsertCompletion");

        assertThat(sql).contains("ON DUPLICATE KEY UPDATE").contains("DEL_YN = 'N'")
                .contains("CMPTN_CYCL = VALUES(CMPTN_CYCL)");
    }

    @Test
    void 완료_기록_삭제는_DEL_YN_UPDATE다() throws Exception {
        assertThat(sql("update", "deleteCompletion")).contains("SET DEL_YN = 'Y'");
    }

    @Test
    void 최근_완료일과_기본_항목_수는_잠금_읽기로_다른_트랜잭션의_최신_기록을_본다() throws Exception {
        assertThat(sql("select", "selectLatestCompletionYmd")).contains("MAX(CMPTN_YMD)").contains("DEL_YN = 'N'")
                .contains("FOR UPDATE");
        assertThat(sql("select", "countBasicItemsForUpdate")).contains("BSC_ARTCL_YN = 'Y'").contains("FOR UPDATE");
    }

    @Test
    void 반려동물별_항목명은_삭제되지_않은_항목을_반려동물과_등록순으로_돌려준다() throws Exception {
        assertThat(sql("select", "selectNamesByPets")).contains("DEL_YN = 'N'")
                .contains("PET_SN IN")
                .contains("ORDER BY PET_SN , CARE_ARTCL_SN");
    }

    @Test
    void 메인_표시_전체_해제는_반려동물의_삭제되지_않은_항목만_바꾼다() throws Exception {
        assertThat(sql("update", "updateMainExposureClear")).contains("PET_SN = #{petSn}").contains("DEL_YN = 'N'");
    }
}
