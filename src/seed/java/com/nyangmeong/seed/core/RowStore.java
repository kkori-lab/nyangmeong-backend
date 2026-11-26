package com.nyangmeong.seed.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.nyangmeong.seed.SeedException;

/**
 * [메모리의 시드 행]
 * - 남겨 둔 행 위에 넣으므로 PK는 테이블의 AUTO_INCREMENT 다음 번호부터 매긴다(지운 번호를 다시 쓰지 않아 감사 로그의 대상 번호와 겹치지 않는다)
 * - 행을 모았다가 flush에서 한 번에 넣고, 남겨 둔 행을 고치는 것(주인공 닉네임 등)은 update로 모아 넣은 뒤 실행한다
 * - 넣을 때는 FK 검사를 끄고, 검사는 SeedValidator가 한다
 */
public final class RowStore {

    private static final int BATCH_SIZE = 500;
    private static final String SEED_ACTOR = "SEED";

    private record Update(String table, String pkColumn, long sn, Map<String, Object> values) {
    }

    private final Map<String, List<Row>> tables = new LinkedHashMap<>();
    /** 테이블 → 첫 행 번호 - 1 */
    private final Map<String, Long> bases;
    private final List<Update> updates = new ArrayList<>();

    private RowStore(Map<String, Long> bases) {
        this.bases = bases;
    }

    /**
     * 테이블마다 다음 번호 읽기
     * - AUTO_INCREMENT 값(지운 행 번호까지 지나간 값)과 MAX(PK) 중 큰 쪽 다음부터
     */
    public static RowStore load(Connection conn, Schema schema) throws SQLException {
        Map<String, Long> bases = new HashMap<>();
        try (Statement st = conn.createStatement()) {
            st.execute("SET SESSION information_schema_stats_expiry = 0");
            try (ResultSet rs = st.executeQuery("""
                    SELECT UPPER(TABLE_NAME), AUTO_INCREMENT FROM information_schema.TABLES
                     WHERE TABLE_SCHEMA = DATABASE() AND AUTO_INCREMENT IS NOT NULL""")) {
                while (rs.next()) {
                    bases.put(rs.getString(1), rs.getLong(2) - 1);
                }
            }
            for (String table : schema.tableNames()) {
                String pk = schema.singlePrimaryKey(table);
                if (pk == null) {
                    continue;
                }
                try (ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(" + pk + "), 0) FROM " + table)) {
                    rs.next();
                    bases.merge(table, rs.getLong(1), Math::max);
                }
            }
        }
        return new RowStore(bases);
    }

    /**
     * 행 추가
     * - PK를 매기고 fill로 컬럼을 채운다
     */
    public Row add(String table, Consumer<Row> fill) {
        List<Row> rows = tables.computeIfAbsent(table, k -> new ArrayList<>());
        Row row = new Row(table, table + "_SN", base(table) + rows.size() + 1L);
        fill.accept(row);
        rows.add(row);
        return row;
    }

    /**
     * 남겨 둔 행 고치기(넣기가 끝난 뒤 실행)
     */
    public void update(String table, long sn, Map<String, Object> values) {
        updates.add(new Update(table, table + "_SN", sn, new LinkedHashMap<>(values)));
    }

    public Set<String> tableNames() {
        return tables.keySet();
    }

    public List<Row> rows(String table) {
        return tables.getOrDefault(table, List.of());
    }

    public Row row(String table, long sn) {
        return rows(table).get((int) (sn - base(table) - 1));
    }

    public Map<String, Integer> counts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        tables.forEach((table, rows) -> counts.put(table, rows.size()));
        return counts;
    }

    public long totalRows() {
        return tables.values().stream().mapToLong(List::size).sum();
    }

    /**
     * 넣은 테이블 → 첫 행 번호(검사가 넣은 행만 보게)
     */
    public Map<String, Long> firstSns() {
        Map<String, Long> first = new LinkedHashMap<>();
        tables.keySet().forEach(table -> first.put(table, base(table) + 1));
        return first;
    }

    public int updateCount() {
        return updates.size();
    }

    private long base(String table) {
        return bases.getOrDefault(table, 0L);
    }

    /**
     * 행이 있는 테이블
     */
    public static Map<String, Long> nonEmpty(Connection conn, List<String> tables) throws SQLException {
        Map<String, Long> filled = new LinkedHashMap<>();
        try (Statement st = conn.createStatement()) {
            for (String table : tables) {
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    rs.next();
                    if (rs.getLong(1) > 0) {
                        filled.put(table, rs.getLong(1));
                    }
                }
            }
        }
        return filled;
    }

    /**
     * 넣기 전 컬럼 점검
     * - DB에 없는 컬럼을 넣거나, 값이 꼭 필요한 컬럼을 빠뜨리면 테이블·컬럼을 짚어 멈춘다(자동으로 채우지 않는다)
     */
    public void checkColumns(Schema schema) {
        List<String> problems = new ArrayList<>();
        tables.forEach((table, rows) -> {
            if (!schema.hasTable(table)) {
                problems.add(table + " 테이블이 DB에 없음");
                return;
            }
            Map<String, Schema.Column> columns = schema.columns(table);
            Set<String> given = new LinkedHashSet<>();
            rows.forEach(row -> given.addAll(row.values().keySet()));
            for (String name : given) {
                Schema.Column column = columns.get(name);
                if (column == null) {
                    problems.add(table + "." + name + " 컬럼이 DB에 없음");
                } else if (column.generated()) {
                    problems.add(table + "." + name + " 생성 컬럼에 값을 넣음");
                }
            }
            for (Schema.Column column : columns.values()) {
                if (column.requiresValue() && rows.stream().anyMatch(row -> !row.values().containsKey(column.name()))) {
                    problems.add(table + "." + column.name() + " NOT NULL인데 값이 없음");
                }
            }
        });
        for (Update update : updates) {
            Map<String, Schema.Column> columns = schema.columns(update.table());
            update.values().keySet().stream().filter(name -> !columns.containsKey(name))
                    .forEach(name -> problems.add(update.table() + "." + name + " 컬럼이 DB에 없음(고치기)"));
        }
        if (!problems.isEmpty()) {
            throw new SeedException("DB 구조와 생성기가 맞지 않습니다. src/seed의 해당 Seeder를 고쳐 주세요\n  - "
                    + String.join("\n  - ", problems));
        }
    }

    /**
     * DB에 넣기
     * - 호출한 쪽의 트랜잭션 안에서 실행. 컬럼 구성이 같은 행끼리 묶어 배치로 넣고, 남겨 둔 행 고치기를 실행한다
     */
    public void flush(Connection conn, Schema schema) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 0");
        }
        try {
            for (Map.Entry<String, List<Row>> entry : tables.entrySet()) {
                insertTable(conn, schema, entry.getKey(), entry.getValue());
            }
            for (Update update : updates) {
                runUpdate(conn, update);
            }
        } finally {
            try (Statement st = conn.createStatement()) {
                st.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        }
    }

    private void insertTable(Connection conn, Schema schema, String table, List<Row> tableRows) throws SQLException {
        boolean registrant = schema.columns(table).containsKey("RGTR_ID");
        boolean modifier = schema.columns(table).containsKey("MDFR_ID");
        boolean modified = schema.columns(table).containsKey("MDFCN_DT");
        boolean registered = schema.columns(table).containsKey("REG_DT");
        Map<List<String>, List<Row>> groups = new LinkedHashMap<>();
        for (Row row : tableRows) {
            String actor = row.actor() == null ? SEED_ACTOR : String.valueOf(row.actor());
            if (registrant) {
                row.setIfAbsent("RGTR_ID", actor);
            }
            if (modifier) {
                row.setIfAbsent("MDFR_ID", actor);
            }
            if (!registered) {
                row.values().remove("REG_DT");
            }
            if (modified && row.values().containsKey("REG_DT")) {
                row.setIfAbsent("MDFCN_DT", row.get("REG_DT"));
            }
            groups.computeIfAbsent(List.copyOf(row.values().keySet()), k -> new ArrayList<>()).add(row);
        }
        for (Map.Entry<List<String>, List<Row>> group : groups.entrySet()) {
            insert(conn, table, group.getKey(), group.getValue());
        }
    }

    private void insert(Connection conn, String table, List<String> columns, List<Row> rows) throws SQLException {
        String sql = "INSERT INTO " + table + " (" + String.join(", ", columns) + ") VALUES ("
                + String.join(", ", columns.stream().map(c -> "?").toList()) + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int pending = 0;
            for (Row row : rows) {
                for (int i = 0; i < columns.size(); i++) {
                    ps.setObject(i + 1, row.get(columns.get(i)));
                }
                ps.addBatch();
                if (++pending == BATCH_SIZE) {
                    ps.executeBatch();
                    pending = 0;
                }
            }
            if (pending > 0) {
                ps.executeBatch();
            }
        } catch (SQLException e) {
            throw new SQLException(table + " 넣기 실패: " + e.getMessage(), e.getSQLState(), e.getErrorCode(), e);
        }
    }

    private void runUpdate(Connection conn, Update update) throws SQLException {
        List<String> columns = new ArrayList<>(update.values().keySet());
        String sql = "UPDATE " + update.table() + " SET " + String.join(", ", columns.stream().map(c -> c + " = ?").toList())
                + " WHERE " + update.pkColumn() + " = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < columns.size(); i++) {
                ps.setObject(i + 1, update.values().get(columns.get(i)));
            }
            ps.setLong(columns.size() + 1, update.sn());
            if (ps.executeUpdate() != 1) {
                throw new SeedException(update.table() + " " + update.sn() + "번 행이 없어 고치지 못했습니다");
            }
        }
    }
}
