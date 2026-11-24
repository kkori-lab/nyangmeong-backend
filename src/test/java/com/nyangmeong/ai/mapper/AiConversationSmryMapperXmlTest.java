package com.nyangmeong.ai.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * [검사 - AI 대화 요약 SQL과 테이블 정의]
 * - DB에 붙지 않고 SQL 문장과 기준 DDL만 확인 (docs/convention/test.md)
 */
class AiConversationSmryMapperXmlTest {

    private static final Path XML = Path.of("src/main/resources/mapper/ai/AiConversationMapper.xml");
    private static final Path DDL = Path.of("db/20261120_create_tables_v0.15.sql");

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
        throw new AssertionError("구문 없음: " + id);
    }

    @Test
    void 등록은_이미_있으면_아무것도_바꾸지_않는다() throws Exception {
        String insert = sql("insert", "insertSmryIfAbsent");
        assertThat(insert).contains("ON DUPLICATE KEY UPDATE AI_CNVRS_SMRY_SN = AI_CNVRS_SMRY_SN")
                .doesNotContain("SMRY_CN_ENC = ").doesNotContain("VALUES(");
    }

    @Test
    void 수정은_새_경계선이_더_클_때만_요약과_경계선을_함께_바꾼다() throws Exception {
        assertThat(sql("update", "updateSmryIfAfter")).contains("SET SMRY_CN_ENC = #{smryCnEnc}")
                .contains(", SMRY_END_MSG_SEQ = #{smryEndMsgSeq}")
                .contains("WHERE AI_CNVRS_ROOM_SN = #{aiCnvrsRoomSn}")
                .contains("AND SMRY_END_MSG_SEQ < #{smryEndMsgSeq}");
    }

    @Test
    void 마지막_메시지_순서는_메시지가_없으면_0이다() throws Exception {
        assertThat(sql("select", "selectLastMsgSeq")).contains("COALESCE(MAX(MSG_SEQ), 0)")
                .contains("WHERE AI_CNVRS_ROOM_SN = #{aiCnvrsRoomSn}");
    }

    @Test
    void 대화방을_지우면_요약도_지워지도록_외래키가_CASCADE다() throws Exception {
        String ddl = Files.readString(DDL);
        assertThat(ddl).contains("UNIQUE KEY `UK_AI_CNVRS_SMRY_1` (`AI_CNVRS_ROOM_SN`)");
        assertThat(ddl).contains("ALTER TABLE `AI_CNVRS_SMRY` ADD CONSTRAINT `FK_AI_CNVRS_SMRY_AI_CNVRS_ROOM` "
                + "FOREIGN KEY (`AI_CNVRS_ROOM_SN`) REFERENCES `AI_CNVRS_ROOM` (`AI_CNVRS_ROOM_SN`) "
                + "ON DELETE CASCADE ON UPDATE RESTRICT;");
    }
}
