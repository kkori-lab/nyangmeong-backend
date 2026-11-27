package com.nyangmeong.seed.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.nyangmeong.seed.core.Codes;
import com.nyangmeong.seed.core.Masters;
import com.nyangmeong.seed.core.Row;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.World;

/**
 * [케어미션·포인트]
 * - 반려동물마다 하루 8개, 합계 60P(CareMissionService와 같은 규칙) : 등급 조합 (어려움, 보통, 쉬움) = (0,4,4)·(1,2,5)·(2,0,6),
 *   못 만들면 후보 전체(최대 8개)에 등급 가중치로 60P를 나눈다. 케어리스트 미션은 케어리스트에 고른 항목만
 * - 완료는 실제로 남긴 기록에서 정한다 : 그날 일상기록·케어 완료·지출·일정·글·댓글이 있으면 완료, 식사량은 2번 기록해야 완료
 * - 지난 날은 정산됨(미완료는 진행률만큼 지급), 오늘은 정산 전
 * - 포인트 원장 : 미션 지급(MSN-번호)과 검진기록 등록 20P(CKUP-반려동물-날짜)를 시간 순으로 쌓고 보유 포인트를 맞춘다
 * - 이 Seeder는 다른 Seeder가 만든 행을 읽으므로 맨 마지막에 돈다
 */
public class CareMissionSeeder implements Seeder {

    private enum Grade {
        EASY(5, 1), NORMAL(10, 2), HARD(15, 3);

        final int point;
        final int weight;

        Grade(int point, int weight) {
            this.point = point;
            this.weight = weight;
        }
    }

    private enum Condition {
        ALWAYS, DOG, OBSERVING, REPRESENTATIVE
    }

    /** 미션 이름 → 등급·조건·케어리스트 항목 이름(없으면 null)·완료로 보는 기록 */
    private record Def(String name, Grade grade, Condition condition, String careItem, String evidence) {
    }

    private static final List<Def> DEFS = List.of(new Def("화장실 청소하기", Grade.EASY, Condition.ALWAYS, "화장실 청소", null),
            new Def("양치하기", Grade.HARD, Condition.ALWAYS, "양치", null), new Def("빗질하기", Grade.NORMAL, Condition.ALWAYS, "빗질", null),
            new Def("발 상태 확인하기", Grade.EASY, Condition.ALWAYS, "발 상태 확인", null),
            new Def("놀이하기", Grade.NORMAL, Condition.ALWAYS, "놀이", null), new Def("체중 기록하기", Grade.EASY, Condition.ALWAYS, null, "체중"),
            new Def("식사량 기록하기", Grade.NORMAL, Condition.ALWAYS, null, "식사량"),
            new Def("음수량 기록하기", Grade.EASY, Condition.ALWAYS, null, "음수량"),
            new Def("배변 기록하기", Grade.EASY, Condition.ALWAYS, null, "배변상태"),
            new Def("가계부 지출 등록하기", Grade.EASY, Condition.ALWAYS, null, "지출"),
            new Def("케어 캘린더 일정 등록하기", Grade.EASY, Condition.ALWAYS, null, "일정"),
            new Def("귀 청소하기", Grade.NORMAL, Condition.ALWAYS, "귀 청소", null), new Def("발톱 관리하기", Grade.HARD, Condition.ALWAYS, "발톱 관리", null),
            new Def("목욕하기", Grade.HARD, Condition.ALWAYS, "목욕", null), new Def("눈곱 닦기", Grade.EASY, Condition.ALWAYS, "눈곱 닦기", null),
            new Def("발바닥 털 다듬기", Grade.HARD, Condition.ALWAYS, "발바닥 털 다듬기", null),
            new Def("잇몸·치석 확인하기", Grade.EASY, Condition.ALWAYS, "잇몸·치석 확인", null),
            new Def("산책 기록하기", Grade.HARD, Condition.DOG, null, "산책"),
            new Def("관찰 기록 추가하기", Grade.NORMAL, Condition.OBSERVING, null, "관찰"),
            new Def("펫이야기 글 등록하기", Grade.HARD, Condition.REPRESENTATIVE, null, "글"),
            new Def("펫이야기 댓글 작성하기", Grade.EASY, Condition.REPRESENTATIVE, null, "댓글"));

    private static final int[][] COMBOS = { { 0, 4, 4 }, { 1, 2, 5 }, { 2, 0, 6 } };
    private static final int DAILY_TOTAL = 60;
    private static final int ISSUE_COUNT = 8;
    private static final int CHECKUP_POINT = 20;
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 반려동물·날짜·근거 → 그날 남긴 기록 시각과 남긴 회원 */
    private final Map<String, List<Evidence>> evidence = new HashMap<>();

    private record Evidence(LocalDateTime at, long mbrSn) {
    }

    private record Ledger(long mbrSn, LocalDateTime at, int delta, Long dayMsnSn, String type, String memo, String idmpt) {
    }

    private Seed s;
    private Codes c;

    @Override
    public void run(Seed seed, World w) {
        s = seed;
        c = s.codes();
        collect(w);
        List<Ledger> ledgers = new ArrayList<>();
        for (World.Pet pet : w.pets) {
            if (w.member(pet.ownerSn()).withdrawn()) {
                continue;
            }
            Set<String> careItems = visibleCareItems(pet.sn());
            boolean representative = Long.valueOf(pet.sn()).equals(w.representativePet.get(pet.ownerSn()));
            LocalDate from = pet.registeredAt().toLocalDate().plusDays(1);
            if (from.isBefore(s.today().minusDays(90))) {
                from = s.today().minusDays(90);
            }
            for (LocalDate d = from; !d.isAfter(s.today()); d = d.plusDays(1)) {
                boolean observing = w.observing.getOrDefault(pet.sn(), false) && !d.isBefore(s.today().minusDays(1));
                issue(pet, d, careItems, representative, observing, ledgers);
            }
        }
        for (World.Visit v : w.visits) {
            LocalDateTime at = v.date().atTime(21, s.between(0, 59));
            ledgers.add(new Ledger(v.ownerSn(), at.isAfter(s.now()) ? s.now() : at, CHECKUP_POINT, null, "검진기록 등록",
                    w.pet(v.petSn()).name(), "CKUP-" + v.petSn() + "-" + v.date().format(YMD)));
        }
        ledger(w, ledgers);
    }

    // ---- 근거 모으기

    private void collect(World w) {
        for (Row r : s.store().rows("DAILY_HLTH_RCD")) {
            String indicator = c.name("HLTG09", (String) r.get("DAILY_RCD_IDCT_CD"));
            add((Long) r.get("PET_SN"), (LocalDateTime) r.get("RCD_DT"), indicator, r.actor());
        }
        Map<Long, Row> careBySn = new HashMap<>();
        s.store().rows("CARE_ARTCL").forEach(r -> careBySn.put(r.sn(), r));
        for (Row r : s.store().rows("CARE_CMPTN_HSTRY")) {
            Row care = careBySn.get((Long) r.get("CARE_ARTCL_SN"));
            if (care != null && "Y".equals(care.get("BSC_ARTCL_YN"))) {
                add((Long) care.get("PET_SN"), (LocalDateTime) r.get("REG_DT"), "케어:" + care.get("CARE_ARTCL_NM"), (Long) r.get("MBR_SN"));
            }
        }
        for (Row r : s.store().rows("EXPND")) {
            if (r.get("PET_SN") != null) {
                add((Long) r.get("PET_SN"), (LocalDateTime) r.get("REG_DT"), "지출", (Long) r.get("MBR_SN"));
            }
        }
        String direct = c.of("HLTG22", "직접 등록");
        for (Row r : s.store().rows("SCHDL")) {
            if (direct.equals(r.get("SCHDL_SRC_CD"))) {
                add((Long) r.get("PET_SN"), (LocalDateTime) r.get("REG_DT"), "일정", r.actor());
            }
        }
        String story = c.of("CMUG01", "펫일상");
        Set<Long> storyPosts = new HashSet<>();
        for (Row r : s.store().rows("PST")) {
            if (story.equals(r.get("BBS_CD"))) {
                storyPosts.add(r.sn());
                Long pet = w.representativePet.get((Long) r.get("MBR_SN"));
                if (pet != null) {
                    add(pet, (LocalDateTime) r.get("REG_DT"), "글", (Long) r.get("MBR_SN"));
                }
            }
        }
        for (Row r : s.store().rows("CMNT")) {
            Long pet = w.representativePet.get((Long) r.get("MBR_SN"));
            if (pet != null && storyPosts.contains((Long) r.get("PST_SN")) && !"Y".equals(r.get("SYS_AUTO_CRT_YN"))) {
                add(pet, (LocalDateTime) r.get("REG_DT"), "댓글", (Long) r.get("MBR_SN"));
            }
        }
    }

    private void add(long petSn, LocalDateTime at, String kind, Long mbrSn) {
        evidence.computeIfAbsent(petSn + "|" + at.toLocalDate() + "|" + kind, k -> new ArrayList<>())
                .add(new Evidence(at, mbrSn == null ? 0 : mbrSn));
    }

    private List<Evidence> evidenceOf(long petSn, LocalDate day, String kind) {
        List<Evidence> list = new ArrayList<>(evidence.getOrDefault(petSn + "|" + day + "|" + kind, List.of()));
        list.sort(Comparator.comparing(Evidence::at));
        return list;
    }

    private Set<String> visibleCareItems(long petSn) {
        Set<String> names = new HashSet<>();
        for (Row r : s.store().rows("CARE_ARTCL")) {
            if (((Long) r.get("PET_SN")) == petSn && "Y".equals(r.get("BSC_ARTCL_YN")) && "Y".equals(r.get("INDCT_YN"))) {
                names.add((String) r.get("CARE_ARTCL_NM"));
            }
        }
        return names;
    }

    // ---- 출제·완료·정산

    private void issue(World.Pet pet, LocalDate day, Set<String> careItems, boolean representative, boolean observing,
            List<Ledger> ledgers) {
        Masters m = s.masters();
        Map<Grade, List<Def>> byGrade = new EnumMap<>(Grade.class);
        for (Grade g : Grade.values()) {
            byGrade.put(g, new ArrayList<>());
        }
        for (Def def : DEFS) {
            boolean ok = m.hasMission(def.name()) && switch (def.condition()) {
            case ALWAYS -> true;
            case DOG -> pet.dog();
            case OBSERVING -> observing;
            case REPRESENTATIVE -> representative;
            } && (def.careItem() == null || careItems.contains(def.careItem()));
            if (ok) {
                byGrade.get(def.grade()).add(def);
            }
        }
        byGrade.values().forEach(s::shuffle);
        List<int[]> feasible = new ArrayList<>();
        for (int[] combo : COMBOS) {
            if (byGrade.get(Grade.HARD).size() >= combo[0] && byGrade.get(Grade.NORMAL).size() >= combo[1]
                    && byGrade.get(Grade.EASY).size() >= combo[2]) {
                feasible.add(combo);
            }
        }
        Map<Def, Integer> picked = new LinkedHashMap<>();
        if (!feasible.isEmpty()) {
            int[] combo = s.pick(feasible);
            byGrade.get(Grade.HARD).subList(0, combo[0]).forEach(d -> picked.put(d, Grade.HARD.point));
            byGrade.get(Grade.NORMAL).subList(0, combo[1]).forEach(d -> picked.put(d, Grade.NORMAL.point));
            byGrade.get(Grade.EASY).subList(0, combo[2]).forEach(d -> picked.put(d, Grade.EASY.point));
        } else {
            List<Def> all = new ArrayList<>();
            byGrade.values().forEach(all::addAll);
            s.shuffle(all);
            List<Def> chosen = all.subList(0, Math.min(ISSUE_COUNT, all.size()));
            chosen.sort(Comparator.comparing((Def d) -> d.grade().weight).reversed());
            int weights = chosen.stream().mapToInt(d -> d.grade().weight).sum();
            int given = 0;
            for (Def d : chosen) {
                int p = DAILY_TOTAL * d.grade().weight / weights;
                picked.put(d, p);
                given += p;
            }
            for (int i = 0; given < DAILY_TOTAL; i = (i + 1) % chosen.size(), given++) {
                picked.merge(chosen.get(i), 1, Integer::sum);
            }
        }
        boolean today = day.equals(s.today());
        LocalDateTime issuedAt = day.atTime(0, 0, s.between(1, 40));
        for (Map.Entry<Def, Integer> e : picked.entrySet()) {
            Def def = e.getKey();
            int score = e.getValue();
            Masters.Mission master = m.mission(def.name());
            List<Evidence> done = def.careItem() != null ? evidenceOf(pet.sn(), day, "케어:" + def.careItem())
                    : "관찰".equals(def.evidence()) ? List.of() : evidenceOf(pet.sn(), day, def.evidence());
            int achv = master.achvNmtm();
            int progress = Math.min(done.size(), achv);
            boolean complete = progress >= achv;
            Evidence finisher = complete ? done.get(achv - 1) : null;
            int give = complete ? score : today ? 0 : score * progress / achv;
            Row row = s.store().add("DAY_MSN", r -> r.set("CARE_MSN_SN", master.sn()).set("PET_SN", pet.sn())
                    .set("MBR_SN", pet.ownerSn()).set("CMPTN_MBR_SN", finisher == null ? null : finisher.mbrSn())
                    .set("MSN_CRTR_YMD", day).set("PRGRS_NMTM", progress).set("GIVE_PNT", give).set("SCOR_PNT", score)
                    .set("ACHV_CRTR_NMTM", achv).set("CMPTN_DT", finisher == null ? null : finisher.at()).yn("CMPTN_YN", complete)
                    .yn("CLCLN_CFMTN_YN", !today).set("REG_DT", issuedAt));
            if (give > 0) {
                LocalDateTime at = complete ? finisher.at() : day.plusDays(1).atTime(0, 0, s.between(1, 59));
                ledgers.add(new Ledger(pet.ownerSn(), at, give, row.sn(), "케어미션 달성", pet.name() + " · " + def.name(),
                        "MSN-" + row.sn()));
            }
        }
    }

    private void ledger(World w, List<Ledger> ledgers) {
        ledgers.sort(Comparator.comparing(Ledger::at));
        Map<Long, Integer> balance = new HashMap<>();
        for (Ledger l : ledgers) {
            int after = balance.merge(l.mbrSn(), l.delta(), Integer::sum);
            s.store().add("PNT_GNLGR", r -> r.set("MBR_SN", l.mbrSn()).set("DAY_MSN_SN", l.dayMsnSn())
                    .set("PNT_DLNG_TYPE_CD", c.of("MYPG03", l.type())).set("DLNG_PNT", l.delta()).set("DLNG_AFTR_BLNC_PNT", after)
                    .set("MEMO_CN", l.memo()).set("IDMPT_DMND_ID", l.idmpt()).set("REG_DT", l.at()));
        }
        Map<Long, Row> newMembers = new HashMap<>();
        s.store().rows("MBR").forEach(r -> newMembers.put(r.sn(), r));
        balance.forEach((mbrSn, points) -> {
            Row created = newMembers.get(mbrSn);
            if (created != null) {
                created.set("HLD_PNT", points);
            } else {
                s.store().update("MBR", mbrSn, Map.of("HLD_PNT", points));
            }
        });
    }
}
