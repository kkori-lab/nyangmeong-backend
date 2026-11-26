package com.nyangmeong.seed.core;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.nyangmeong.seed.SeedException;

/**
 * [남겨 둔 기준값·회원]
 * - 시드는 기준값(품종·지역·장소·케어미션·약관·운영 설정)을 만들지 않고 DB에 남아 있는 것을 읽어 쓴다
 * - 남긴 회원(관리자·테스트 계정·기록이 가리키는 회원)도 읽어 그 위에 데이터를 넣는다
 */
public final class Masters {

    public record Variety(long sn, String petSeCd, String name) {
    }

    public record Region(long sn, String stepCd, String ctpvNm, String sggNm, String emdNm, Long upSn) {
    }

    public record Place(long sn, String clsfCd, String name, String addr, BigDecimal lat, BigDecimal lot) {
    }

    public record Mission(long sn, String poolCd, String name, int achvNmtm) {
    }

    public record Terms(long sn, String key, boolean essential) {
    }

    public record KeptMember(long sn, String nickname, String statusCd, String authorityCd, String joinPathCd,
            LocalDateTime joinedAt, String emailEnc, boolean createdBySeed, LocalDateTime withdrawnAt) {
    }

    private final Map<String, Variety> varieties = new LinkedHashMap<>();
    private final List<Region> regions = new ArrayList<>();
    private final List<Place> places = new ArrayList<>();
    private final Map<String, Mission> missions = new LinkedHashMap<>();
    private final List<Terms> terms = new ArrayList<>();
    private final Map<Long, KeptMember> members = new LinkedHashMap<>();
    private final Map<String, Object> settings = new LinkedHashMap<>();

    private Masters() {
    }

    static Masters load(Connection conn) throws SQLException {
        Masters m = new Masters();
        query(conn, "SELECT VRTY_SN, PET_SE_CD, VRTY_NM FROM VRTY WHERE USE_YN = 'Y'",
                rs -> m.varieties.put(rs.getString(2) + ":" + rs.getString(3),
                        new Variety(rs.getLong(1), rs.getString(2), rs.getString(3))));
        query(conn, "SELECT RGN_SN, RGN_STP_CD, CTPV_NM, SGG_NM, EMD_NM, UP_RGN_SN FROM RGN WHERE USE_YN = 'Y'",
                rs -> m.regions.add(new Region(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getObject(6, Long.class))));
        query(conn, "SELECT PLC_SN, PLC_CLSF_CD, PLC_CONM, PLC_ADDR, PLC_LAT, PLC_LOT FROM PLC WHERE USE_YN = 'Y'",
                rs -> m.places.add(new Place(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getBigDecimal(5), rs.getBigDecimal(6))));
        query(conn, "SELECT CARE_MSN_SN, MSN_POOL_CD, MSN_NM, ACHV_CRTR_NMTM FROM CARE_MSN WHERE USE_YN = 'Y'",
                rs -> m.missions.put(rs.getString(3),
                        new Mission(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getInt(4))));
        query(conn, "SELECT TRMS_SN, TRMS_FIX_IDNTF_KEY, ESNTL_AGRE_YN FROM TRMS WHERE USE_YN = 'Y'",
                rs -> m.terms.add(new Terms(rs.getLong(1), rs.getString(2), "Y".equals(rs.getString(3)))));
        query(conn, """
                SELECT MBR_SN, MBR_NCNM, MBR_STTS_CD, MBR_AUTHRT_CD, JOIN_PATH_CD, REG_DT, ACNT_EML_ENC, RGTR_ID, WHDWL_DT
                  FROM MBR ORDER BY MBR_SN""",
                rs -> m.members.put(rs.getLong(1), new KeptMember(rs.getLong(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getTimestamp(6).toLocalDateTime(), rs.getString(7),
                        "SEED".equals(rs.getString(8)),
                        rs.getTimestamp(9) == null ? null : rs.getTimestamp(9).toLocalDateTime())));
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM SYS_STNG"); ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new SeedException("SYS_STNG이 비어 있습니다");
            }
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                m.settings.put(rs.getMetaData().getColumnName(i).toUpperCase(), rs.getObject(i));
            }
        }
        if (m.varieties.isEmpty() || m.regions.isEmpty() || m.missions.isEmpty() || m.terms.isEmpty()) {
            throw new SeedException("기준값(VRTY·RGN·CARE_MSN·TRMS)이 비어 있습니다. 시드는 기준값을 만들지 않습니다");
        }
        return m;
    }

    private interface RowReader {
        void read(ResultSet rs) throws SQLException;
    }

    private static void query(Connection conn, String sql, RowReader reader) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                reader.read(rs);
            }
        }
    }

    /**
     * 품종 찾기. 없으면 멈춘다(관리자가 품종명을 바꾸면 Seeder도 고친다)
     */
    public Variety variety(String petSeCd, String name) {
        Variety v = varieties.get(petSeCd + ":" + name);
        if (v == null) {
            throw new SeedException("품종 '" + name + "'이 VRTY에 없습니다");
        }
        return v;
    }

    /**
     * 읍면동(없으면 시군구) 지역 찾기
     */
    public Region region(String sggNm, String emdNm) {
        return regions.stream()
                .filter(r -> sggNm.equals(r.sggNm()) && (emdNm == null ? r.emdNm() == null : emdNm.equals(r.emdNm())))
                .findFirst()
                .orElseThrow(() -> new SeedException("지역 '" + sggNm + " " + (emdNm == null ? "" : emdNm) + "'이 RGN에 없습니다"));
    }

    /**
     * 주소에 이 글자가 든 장소. 옛 시드가 만든 'n호점' 이름은 빼고 고른다
     */
    public List<Place> placesIn(String addrPart) {
        return places.stream().filter(p -> p.addr() != null && p.addr().contains(addrPart))
                .filter(p -> !p.name().matches(".*\\d+호점$")).toList();
    }

    public Mission mission(String name) {
        Mission mission = missions.get(name);
        if (mission == null) {
            throw new SeedException("케어미션 '" + name + "'이 CARE_MSN에 없거나 사용 중이 아닙니다");
        }
        return mission;
    }

    public boolean hasMission(String name) {
        return missions.containsKey(name);
    }

    public List<Terms> terms() {
        return terms;
    }

    public KeptMember member(long sn) {
        KeptMember member = members.get(sn);
        if (member == null) {
            throw new SeedException("남긴 회원 " + sn + "번이 MBR에 없습니다. seedClean이 남기는 회원인지 확인해 주세요");
        }
        return member;
    }

    public boolean hasMember(long sn) {
        return members.containsKey(sn);
    }

    public List<KeptMember> members() {
        return new ArrayList<>(members.values());
    }

    public String stringSetting(String column) {
        Object value = settings.get(column);
        return value == null ? null : value.toString();
    }

    public int intSetting(String column) {
        Object value = settings.get(column);
        if (value == null) {
            throw new SeedException("SYS_STNG." + column + " 값이 없습니다");
        }
        return ((Number) value).intValue();
    }
}
