package com.nyangmeong.seed;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.nyangmeong.seed.core.Schema;
import com.nyangmeong.seed.core.SeedValidator;
import com.nyangmeong.seed.core.SeedWarnings;

/**
 * [업무 데이터 골라 비우기]
 * - DB를 다시 만들지 않고 SeedTables 목록대로 지운다. 호출한 쪽의 트랜잭션 안에서 실행
 * - 남기는 회원 : 시드가 만들지 않은 회원(팀원 테스트 계정), 관리자, 남기는 테이블(관리자 기록 등)이 가리키는 회원,
 *   SeedTables.KEPT_MEMBERS(시드가 만들었지만 사람이 남기기로 한 계정)
 * - 남기는 첨부파일 : 남기는 테이블·남긴 회원·남긴 공지 본문이 가리키는 파일. 지운 행의 서버 파일은 그대로 둔다
 * - 남긴 회원의 보유 포인트는 원장을 지우므로 0으로 맞춘다
 * - 장소는 옛 시드가 만든 가짜(이름이 'n호점'으로 끝나거나 카카오 장소 ID가 fake-로 시작)만 지운다. 장소를 가리키는 즐겨찾기는 이미 비운다
 */
public final class SeedCleaner {

    private static final Pattern NOTICE_FILE = Pattern.compile("/api/files/(?:public/)?(\\d+)");

    private final Connection conn;
    private final Schema schema;

    public SeedCleaner(Connection conn, Schema schema) {
        this.conn = conn;
        this.schema = schema;
    }

    /**
     * @return 테이블 → 지운 행 수(0행 제외)
     */
    public Map<String, Long> clean() throws SQLException {
        Set<Long> members = keptMembers();
        Set<Long> files = keptFiles(members);
        System.out.println("[시드] 남기는 회원 " + members.size() + "명: " + members);
        System.out.println("[시드] 남기는 첨부파일 " + files.size() + "개");
        for (String table : SeedTables.unknown(schema)) {
            SeedWarnings.add("새 테이블 " + table + "은 어느 목록에도 없어 지우지 않음(SeedTables에 넣어 주세요)");
        }

        Map<String, Long> deleted = new LinkedHashMap<>();
        try (Statement st = conn.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 0");
            try {
                for (String table : SeedTables.cleared(schema)) {
                    put(deleted, table, st.executeLargeUpdate("DELETE FROM " + table));
                }
                String memberList = list(members);
                for (String table : SeedTables.MEMBER_ACCOUNT) {
                    if (schema.hasTable(table)) {
                        put(deleted, table, st.executeLargeUpdate("DELETE FROM " + table + " WHERE MBR_SN NOT IN (" + memberList + ")"));
                    }
                }
                put(deleted, "MBR", st.executeLargeUpdate("DELETE FROM MBR WHERE MBR_SN NOT IN (" + memberList + ")"));
                put(deleted, "NTC_MTTR", st.executeLargeUpdate(
                        "DELETE FROM NTC_MTTR WHERE NTC_MTTR_SN NOT IN (" + list(SeedTables.KEPT_NOTICES) + ")"));
                put(deleted, "FAQ", st.executeLargeUpdate("DELETE FROM FAQ WHERE FAQ_SN NOT IN (" + list(SeedTables.KEPT_FAQS) + ")"));
                put(deleted, "ATCH_FILE", st.executeLargeUpdate(files.isEmpty() ? "DELETE FROM ATCH_FILE"
                        : "DELETE FROM ATCH_FILE WHERE ATCH_FILE_SN NOT IN (" + list(files) + ")"));
                put(deleted, "PLC", st.executeLargeUpdate(
                        "DELETE FROM PLC WHERE PLC_CONM REGEXP '[0-9]+호점$' OR KAKAO_PLC_ID LIKE 'fake-%'"));
                st.executeLargeUpdate("UPDATE MBR SET HLD_PNT = 0 WHERE HLD_PNT <> 0");
            } finally {
                st.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        }
        List<String> touched = new ArrayList<>(SeedTables.cleared(schema));
        touched.addAll(SeedTables.PARTLY_KEPT);
        new SeedValidator(conn, schema).validateClean(touched);
        return deleted;
    }

    private Set<Long> keptMembers() throws SQLException {
        Set<Long> kept = new TreeSet<>(longs("""
                SELECT m.MBR_SN FROM MBR m
                  JOIN COM_CD c ON c.COM_CD = m.MBR_AUTHRT_CD
                 WHERE m.RGTR_ID <> 'SEED' OR c.COM_CD_NM = '관리자'"""));
        kept.addAll(longs("SELECT MBR_SN FROM MBR WHERE MBR_SN IN (" + list(SeedTables.KEPT_MEMBERS) + ")"));
        for (Schema.ForeignKey fk : schema.foreignKeys()) {
            if (fk.refTable().equals("MBR") && SeedTables.KEPT.contains(fk.table())) {
                kept.addAll(longs("SELECT DISTINCT " + fk.column() + " FROM " + fk.table() + " WHERE " + fk.column() + " IS NOT NULL"));
            }
        }
        return kept;
    }

    private Set<Long> keptFiles(Set<Long> members) throws SQLException {
        Set<Long> kept = new TreeSet<>();
        for (Schema.ForeignKey fk : schema.foreignKeys()) {
            if (fk.refTable().equals("ATCH_FILE") && SeedTables.KEPT.contains(fk.table())) {
                kept.addAll(longs("SELECT DISTINCT " + fk.column() + " FROM " + fk.table() + " WHERE " + fk.column() + " IS NOT NULL"));
            }
        }
        kept.addAll(longs("SELECT ATCH_FILE_SN FROM MBR WHERE ATCH_FILE_SN IS NOT NULL AND MBR_SN IN (" + list(members) + ")"));
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT NTC_MTTR_CN FROM NTC_MTTR WHERE NTC_MTTR_SN IN (" + list(SeedTables.KEPT_NOTICES) + ")");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Matcher m = NOTICE_FILE.matcher(rs.getString(1));
                while (m.find()) {
                    kept.add(Long.parseLong(m.group(1)));
                }
            }
        }
        return kept;
    }

    private List<Long> longs(String sql) throws SQLException {
        List<Long> values = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                values.add(rs.getLong(1));
            }
        }
        return values;
    }

    private static String list(java.util.Collection<Long> values) {
        return values.isEmpty() ? "NULL" : values.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static void put(Map<String, Long> deleted, String table, long n) {
        if (n > 0) {
            deleted.merge(table, n, Long::sum);
        }
    }
}
