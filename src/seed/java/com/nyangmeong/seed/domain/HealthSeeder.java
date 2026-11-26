package com.nyangmeong.seed.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import com.nyangmeong.seed.core.Codes;
import com.nyangmeong.seed.core.Row;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.World;
import com.nyangmeong.seed.core.World.Activity;

/**
 * [건강기록]
 * - 주인공 반려동물 3마리 : 최근 3개월 일상기록, 검진기록(수치·기타검사·처방·반복 예방), 일정(지난·다가오는), 케어리스트와 완료 이력, 관찰 항목
 * - 엑스트라 : 등록한 날부터 활동량만큼의 일상기록, 케어리스트, 일부는 예방접종 기록
 * - 사진이 꼭 있어야 하는 관찰 주기·첨부는 넣지 않는다(시연에서 사람이 올린다)
 * - CK_DAILY_HLTH_RCD_VAL_ONE : 배변상태만 코드, 나머지는 수치
 */
public class HealthSeeder implements Seeder {

    private record Basic(String name, boolean daily, String cycle, int days, String icon) {
    }

    /** BasicCareItem과 같은 순서·이름·아이콘 */
    private static final List<Basic> BASICS = List.of(new Basic("화장실 청소", true, null, 0, "cleaning_services"),
            new Basic("양치", true, null, 0, "dentistry"), new Basic("빗질", true, null, 0, "brush"),
            new Basic("발 상태 확인", true, null, 0, "task_alt"), new Basic("놀이", true, null, 0, "task_alt"),
            new Basic("귀 청소", false, "1주", 7, "hearing"), new Basic("발톱 관리", false, "2주", 14, "content_cut"),
            new Basic("목욕", false, "한달", 30, "bathtub"), new Basic("눈곱 닦기", false, "2주", 14, "task_alt"),
            new Basic("발바닥 털 다듬기", false, "2주", 14, "content_cut"), new Basic("잇몸·치석 확인", false, "2주", 14, "sentiment_satisfied"));

    /** 반려동물 → 케어리스트에 고른 기본 항목과 완료 확률 */
    private static final Map<String, Map<String, Double>> CHOSEN = Map.of(
            "kong", Map.of("양치", 0.6, "빗질", 0.8, "발 상태 확인", 0.9, "놀이", 0.7, "귀 청소", 0.9, "발톱 관리", 1.0, "눈곱 닦기", 1.0),
            "bori", Map.of("양치", 0.5, "빗질", 0.85, "놀이", 0.8, "귀 청소", 0.8, "발톱 관리", 0.9, "목욕", 1.0, "발바닥 털 다듬기", 0.8),
            "dubu", Map.of("화장실 청소", 0.95, "빗질", 0.5, "놀이", 0.6, "잇몸·치석 확인", 0.8));

    private record Insp(String code, String unit, double low, double high) {
    }

    private static final Map<String, Insp> DOG = Map.ofEntries(Map.entry("WBC", new Insp("WBC", "K/μL", 6.0, 17.0)),
            Map.entry("RBC", new Insp("RBC", "M/μL", 5.5, 8.5)), Map.entry("HGB", new Insp("HGB", "g/dL", 12.0, 18.0)),
            Map.entry("HCT", new Insp("HCT", "%", 37.0, 55.0)), Map.entry("PLT", new Insp("PLT", "K/μL", 175.0, 500.0)),
            Map.entry("BUN", new Insp("BUN", "mg/dL", 7.0, 27.0)), Map.entry("CREA", new Insp("CREA", "mg/dL", 0.5, 1.8)),
            Map.entry("ALT", new Insp("ALT", "U/L", 10.0, 125.0)), Map.entry("ALP", new Insp("ALP", "U/L", 23.0, 212.0)),
            Map.entry("GLU", new Insp("GLU", "mg/dL", 70.0, 143.0)));
    private static final Map<String, Insp> CAT = Map.of("BUN", new Insp("BUN", "mg/dL", 16.0, 36.0),
            "CREA", new Insp("CREA", "mg/dL", 0.8, 2.4), "SDMA", new Insp("SDMA", "μg/dL", 0.0, 14.0),
            "HCT", new Insp("HCT", "%", 30.0, 45.0), "GLU", new Insp("GLU", "mg/dL", 71.0, 159.0),
            "ALT", new Insp("ALT", "U/L", 12.0, 130.0));

    private Codes c;
    private Seed s;
    private World w;
    /** 휴면·정지 회원의 기록이 끝난 날(없으면 오늘까지) */
    private LocalDate endDay;

    @Override
    public void run(Seed seed, World world) {
        s = seed;
        w = world;
        c = s.codes();
        World.Member persona = w.persona();
        personaDaily();
        personaCheckups(persona.sn());
        personaSchedules();
        for (World.Pet pet : w.petsOf(persona.sn())) {
            careList(pet, persona.sn(), CHOSEN.get(pet.key()), 90);
        }
        observation();
        widgets(persona);
        for (World.Pet pet : w.pets) {
            if (pet.ownerSn() == persona.sn()) {
                continue;
            }
            World.Member owner = w.member(pet.ownerSn());
            if (owner.withdrawn()) {
                continue;
            }
            endDay = owner.inactiveFrom() == null ? null : owner.inactiveFrom().toLocalDate();
            extraDaily(pet, owner.activity());
            if (endDay == null) {
                extraCare(pet, owner);
            }
            endDay = null;
        }
    }

    // ---- 일상기록

    private void personaDaily() {
        dailyRange(w.pet("kong"), 90, 1.0, 60, 72, 160, 220, new int[] { 15, 25 }, 1, 0.92, 3.5, 3.4);
        dailyRange(w.pet("bori"), 90, 1.0, 175, 205, 450, 620, new int[] { 35, 70 }, 2, 0.95, 14.2, 13.3);
        dailyRange(w.pet("dubu"), 90, 1.0, 52, 64, 230, 310, null, 0, 0.9, 4.6, 4.4);
    }

    /**
     * 일상기록
     * @param days     오늘부터 며칠 전까지(등록일 전은 넣지 않는다)
     * @param diligent 기록 확률 배율
     * @param walks    하루 산책 횟수(개)
     */
    private void dailyRange(World.Pet pet, int days, double diligent, int mealLow, int mealHigh, int waterLow, int waterHigh,
            int[] walk, int walks, double stoolNormal, double weightStart, double weightNow) {
        LocalDate end = endDay == null ? s.today() : endDay;
        LocalDate from = later(end.minusDays(days), pet.registeredAt().toLocalDate());
        long span = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(from, end));
        for (LocalDate d = from; !d.isAfter(end); d = d.plusDays(1)) {
            long i = java.time.temporal.ChronoUnit.DAYS.between(from, d);
            boolean today = d.equals(s.today());
            if (i % 4 == 0 || today && diligent >= 1.0) {
                double trend = weightStart + (weightNow - weightStart) * i / span;
                double weight = today ? weightNow : trend + s.between(-0.05, 0.05) * Math.max(1, weightNow / 5);
                record(pet, "체중", s.decimal(weight, 1).doubleValue(), null, d, LocalTime.of(8, s.between(0, 40)));
            }
            // 아침·저녁 두 번 나눠 먹는다(식사량 기록하기 미션은 2번 기록해야 완료)
            if (s.chance(0.88 * diligent)) {
                record(pet, "식사량", (double) Math.round(s.between(mealLow, mealHigh) * 0.5), null, d,
                        LocalTime.of(s.between(7, 8), s.between(0, 59)));
            }
            if (s.chance(0.85 * diligent)) {
                record(pet, "식사량", (double) Math.round(s.between(mealLow, mealHigh) * 0.5), null, d,
                        LocalTime.of(s.between(18, 20), s.between(0, 59)));
            }
            if (s.chance(0.75 * diligent)) {
                record(pet, "음수량", (double) s.between(waterLow, waterHigh), null, d, LocalTime.of(s.between(21, 22), s.between(0, 59)));
            }
            if (s.chance(0.8 * diligent)) {
                String stool = s.chance(stoolNormal) ? "정상" : pet.dog() ? s.pick("무름", "딱딱함") : s.pick("딱딱함", "변비");
                record(pet, "배변상태", null, stool, d, LocalTime.of(s.between(7, 10), s.between(0, 59)));
            }
            for (int k = 0; walk != null && k < walks; k++) {
                if (s.chance(0.85 * diligent)) {
                    LocalTime at = k == 0 ? LocalTime.of(s.between(7, 8), s.between(0, 59)) : LocalTime.of(s.between(18, 20), s.between(0, 59));
                    record(pet, "산책", (double) s.between(walk[0], walk[1]), null, d, at);
                }
            }
        }
    }

    private void record(World.Pet pet, String indicator, Double value, String stool, LocalDate day, LocalTime time) {
        LocalDateTime at = day.atTime(time);
        if (at.isAfter(s.now()) || at.isBefore(pet.registeredAt())) {
            return;
        }
        long actor = actorOf(pet, day);
        s.store().add("DAILY_HLTH_RCD", r -> r.set("PET_SN", pet.sn()).set("DAILY_RCD_IDCT_CD", c.of("HLTG09", indicator))
                .set("CRSD_STTS_CD", stool == null ? null : c.of("HLTG10", stool))
                .set("RCD_NVL", value == null ? null : s.decimal(value, 1)).set("RCD_DT", at).set("REG_DT", at.plusMinutes(s.between(0, 20)))
                .by(actor));
    }

    /** 보리는 공동양육자(보리아빠)가 저녁 기록을 남기기도 한다 */
    private long actorOf(World.Pet pet, LocalDate day) {
        if ("bori".equals(pet.key()) && day.getDayOfMonth() % 3 == 0) {
            return w.member("boriDad").sn();
        }
        return pet.ownerSn();
    }

    private void extraDaily(World.Pet pet, Activity activity) {
        double diligent = switch (activity) {
        case HEAVY -> 0.8;
        case NORMAL -> 0.55;
        default -> 0.3;
        };
        double weight = pet.weight().doubleValue();
        int meal = (int) Math.round(weight * (pet.dog() ? 18 : 14));
        int water = (int) Math.round(weight * 50);
        People.PetPlan plan = plan(pet);
        // 오늘 등록한 반려동물은 등록하면서 체중을 한 번 기록했다
        if (pet.registeredAt().toLocalDate().equals(s.today())) {
            LocalDateTime at = pet.registeredAt().plusMinutes(10);
            if (at.isBefore(s.now())) {
                record(pet, "체중", plan.weightNow(), null, s.today(), at.toLocalTime());
            }
        }
        dailyRange(pet, 60, diligent, (int) (meal * 0.9), (int) (meal * 1.1), (int) (water * 0.8), (int) (water * 1.2),
                pet.dog() ? new int[] { 20, 60 } : null, 1, 0.92, plan.weightStart(), plan.weightNow());
    }

    // ---- 검진기록

    private void personaCheckups(long mbrSn) {
        World.Pet kong = w.pet("kong");
        World.Pet bori = w.pet("bori");
        World.Pet dubu = w.pet("dubu");

        Row k1 = checkup(kong, 82, "10:30", "질병진료", "전북동물의료센터", 68_000,
                "왼쪽 뒷다리를 들고 걸어서 내원. 슬개골 탈구 2기 소견으로 수술보다는 체중 유지, 관절 보조제, 짧은 산책을 권장받음.",
                "미끄럼 방지 매트 깔아 주기");
        prescription(kong, k1, "온시올", 1, 82, 76, true);
        Row k2 = checkup(kong, 62, "11:00", "예방접종", "온누리동물병원", 45_000, "종합백신 추가접종. 심장사상충 예방약을 매달 먹이기로 함.", null);
        prevention(kong, k2, "심장사상충 예방", 62);
        Row k3 = checkup(kong, 35, "15:20", "정기검진", "온누리동물병원", 132_000,
                "노령견 정기검진. ALP가 조금 높아 3개월 뒤 재검 권장. 슬개골은 지난번과 같은 2기.", "다음 검진 때 공복 12시간");
        tests(k3, DOG, Map.of("WBC", 9.8, "RBC", 7.1, "HCT", 46.2, "PLT", 322.0, "BUN", 19.4, "CREA", 0.9, "ALT", 98.0, "ALP", 245.0,
                "GLU", 104.0));
        etc(k3, "슬개골 촉진", "2기 유지, 진행 없음");
        etc(k3, "심장 청진", "잡음 없음");

        Row b1 = checkup(bori, 70, "10:00", "정기검진", "덕진24시동물병원", 86_000,
                "체중 14.2kg으로 과체중(BCS 6/9). 사료량을 10% 줄이고 다이어트 사료로 바꾸기로 함. 목표 12.5kg.", "간식은 하루 3개까지");
        tests(b1, DOG, Map.of("WBC", 11.2, "HCT", 49.5, "BUN", 16.8, "CREA", 1.1, "ALT", 54.0, "GLU", 97.0));
        prevention(bori, b1, "심장사상충 예방", 70);
        checkup(bori, 20, "17:40", "예방접종", "덕진24시동물병원", 30_000, "광견병 예방접종. 접종 후 30분 대기, 특이 반응 없음.", null);

        Row d1 = checkup(dubu, 88, "09:40", "검사", "전북동물의료센터", 185_000,
                "물을 많이 마시고 소변량이 늘어서 내원. 혈액·소변·초음파 결과 만성 신장병 2기 진단. 신장 처방식과 흡착제 시작.",
                "닭고기 들어간 간식 주지 않기");
        tests(d1, CAT, Map.of("BUN", 41.0, "CREA", 2.6, "SDMA", 19.0, "HCT", 33.5, "GLU", 118.0));
        etc(d1, "소변검사", "요비중 1.020, 단백뇨 경계");
        etc(d1, "복부 초음파", "양쪽 신장 크기가 약간 작음");
        prescription(dubu, d1, "레나메진", 2, 88, -4, true);
        Row d2 = checkup(dubu, 28, "09:50", "검사", "전북동물의료센터", 98_000,
                "처방식과 흡착제를 유지한 뒤 수치가 조금 좋아짐. 1개월 뒤 재검.", null);
        tests(d2, CAT, Map.of("BUN", 35.0, "CREA", 2.2, "SDMA", 16.0, "HCT", 34.8));
    }

    private Row checkup(World.Pet pet, int daysAgo, String time, String type, String hospital, int amount, String content,
            String memo) {
        LocalDate day = s.today().minusDays(daysAgo);
        LocalDateTime at = day.atTime(LocalTime.parse(time));
        Row schedule = s.store().add("SCHDL", r -> r.set("PET_SN", pet.sn()).set("SCHDL_KND_CD", c.of("HLTG19", "병원"))
                .set("SCHDL_SRC_CD", c.of("HLTG22", "검진기록 연동")).set("SCHDL_DT", at).set("SCHDL_NM", type)
                .yn("HR_INPT_YN", true).set("HSPTL_NM", hospital).set("CKUP_TYPE_CD", c.of("HLTG01", type))
                .set("REG_DT", at.plusHours(4)).by(pet.ownerSn()));
        LocalDateTime recordedAt = at.plusHours(4).plusMinutes(s.between(0, 50));
        Row record = s.store().add("CKUP_RCD", r -> r.set("SCHDL_SN", schedule.sn()).set("CKUP_TYPE_CD", c.of("HLTG01", type))
                .set("CKUP_RCD_STTS_CD", c.of("HLTG02", "완료")).set("RCD_SRC_CD", c.of("HLTG03", "직접입력"))
                .set("HSPTL_NM", hospital).set("CKUP_CN", content).set("CKUP_MEMO_CN", memo).set("REG_DT", recordedAt)
                .by(pet.ownerSn()));
        s.store().add("CKUP_RCD_STTS_HSTRY", r -> r.set("CKUP_RCD_SN", record.sn()).set("MBR_SN", pet.ownerSn())
                .set("AFTR_STTS_CD", c.of("HLTG02", "완료")).set("REG_DT", recordedAt));
        w.visits.add(new World.Visit(pet.sn(), pet.ownerSn(), day, hospital, type, amount));
        return record;
    }

    private void tests(Row record, Map<String, Insp> refs, Map<String, Double> values) {
        LocalDateTime at = (LocalDateTime) record.get("REG_DT");
        int seq = 0;
        for (String code : List.of("WBC", "RBC", "HGB", "HCT", "PLT", "BUN", "CREA", "SDMA", "ALT", "AST", "ALP", "GLU")) {
            Double value = values.get(code);
            if (value == null) {
                continue;
            }
            Insp insp = refs.get(code);
            int order = seq += 10;
            s.store().add("INSP_NVL", r -> r.set("CKUP_RCD_SN", record.sn()).set("INSP_ARTCL_CD", c.of("HLTG12", code))
                    .set("INSP_ARTCL_DRCT_INPT_NM", "").set("UNIT_NM", insp.unit()).set("RSLT_NVL", s.decimal(value, 4))
                    .set("REF_LLMT_NVL", s.decimal(insp.low(), 4)).set("REF_UPLMT_NVL", s.decimal(insp.high(), 4))
                    .set("SORT_SEQ", order).set("REG_DT", at).by(ownerOf(record)));
        }
    }

    private void etc(Row record, String name, String result) {
        s.store().add("ETC_INSP", r -> r.set("CKUP_RCD_SN", record.sn()).set("INSP_NM", name).set("INSP_RSLT_CN", result)
                .set("REG_DT", record.get("REG_DT")).by(ownerOf(record)));
    }

    private long ownerOf(Row record) {
        long scheduleSn = (Long) record.get("SCHDL_SN");
        long petSn = (Long) s.store().row("SCHDL", scheduleSn).get("PET_SN");
        return w.pet(petSn).ownerSn();
    }

    /**
     * 처방과 케어리스트 연동 항목
     * @param endDaysAgo 복용 끝난 날(음수면 앞으로 남은 날)
     */
    private void prescription(World.Pet pet, Row record, String drug, int perDay, int startDaysAgo, int endDaysAgo,
            boolean reflect) {
        LocalDate start = s.today().minusDays(startDaysAgo);
        LocalDate end = s.today().minusDays(endDaysAgo);
        boolean taking = !end.isBefore(s.today());
        LocalDateTime at = (LocalDateTime) record.get("REG_DT");
        Row prsc = s.store().add("PRSC", r -> r.set("CKUP_RCD_SN", record.sn()).set("PRSC_STTS_CD", c.of("HLTG06", taking ? "복용중" : "종료"))
                .set("MDCN_NM", drug).set("DAY_MDCN_NMTM", perDay).set("MDCN_BGNG_YMD", start).set("MDCN_END_YMD", end)
                .yn("CARE_RFLT_YN", reflect).set("REG_DT", at).by(pet.ownerSn()));
        s.store().add("PRSC_STTS_HSTRY", r -> r.set("PRSC_SN", prsc.sn()).set("MBR_SN", pet.ownerSn())
                .set("AFTR_STTS_CD", c.of("HLTG06", "복용중")).set("REG_DT", at));
        if (!taking) {
            s.store().add("PRSC_STTS_HSTRY", r -> r.set("PRSC_SN", prsc.sn()).set("BFR_STTS_CD", c.of("HLTG06", "복용중"))
                    .set("AFTR_STTS_CD", c.of("HLTG06", "종료")).set("CHG_RSN", "복용 기간 종료").set("REG_DT", end.plusDays(1).atTime(0, 5)));
        }
        if (!reflect) {
            return;
        }
        Row care = s.store().add("CARE_ARTCL", r -> r.set("PRSC_SN", prsc.sn()).set("PET_SN", pet.sn())
                .set("CARE_ZONE_CD", c.of("HLTG13", "일상관리")).set("CARE_ARTCL_NM", drug).set("RPRS_ICON_ID", "medication")
                .yn("BSC_ARTCL_YN", false).yn("DEL_YN", !taking).set("REG_DT", at).by(pet.ownerSn()));
        LocalDate stop = taking ? s.today() : end;
        LocalDate last = null;
        for (LocalDate d = start; !d.isAfter(stop); d = d.plusDays(1)) {
            int times = d.equals(s.today()) ? (s.now().getHour() >= 9 ? 1 : 0) : s.chance(0.9) ? perDay : Math.max(1, perDay - 1);
            if (times > 0) {
                completion(care.sn(), pet.ownerSn(), times, d);
                last = d;
            }
        }
        care.set("LAST_CMPTN_YMD", last);
    }

    /**
     * 반복 예방(한달 주기)과 정기관리 연동 항목. 시작일부터 30일마다 먹였다
     */
    private void prevention(World.Pet pet, Row record, String item, int startDaysAgo) {
        LocalDate start = s.today().minusDays(startDaysAgo);
        LocalDateTime at = (LocalDateTime) record.get("REG_DT");
        Row mng = s.store().add("REPT_PRVNT_MNG", r -> r.set("PET_SN", pet.sn()).set("CKUP_RCD_SN", record.sn())
                .set("REPT_PRVNT_ARTCL_CD", c.of("HLTG07", item)).set("REPT_CYCLE_CD", c.of("HLTG15", "한달")).set("BGNG_YMD", start)
                .set("REG_DT", at).by(pet.ownerSn()));
        LocalDate last = start;
        for (LocalDate d = start; !d.isAfter(s.today()); d = d.plusMonths(1)) {
            completion(-1, pet.ownerSn(), 1, d);
            last = d;
        }
        LocalDate lastDone = last;
        Row care = s.store().add("CARE_ARTCL", r -> r.set("REPT_PRVNT_MNG_SN", mng.sn()).set("PET_SN", pet.sn())
                .set("CARE_ZONE_CD", c.of("HLTG13", "정기관리")).set("CARE_CYCLE_CD", c.of("HLTG15", "한달")).set("CARE_ARTCL_NM", item)
                .set("RPRS_ICON_ID", "vaccines").set("LAST_CMPTN_YMD", lastDone).set("NXTM_PRNMNT_YMD", lastDone.plusMonths(1))
                .yn("BSC_ARTCL_YN", false).set("REG_DT", at).by(pet.ownerSn()));
        fixPending(care.sn());
    }

    // ---- 일정

    private void personaSchedules() {
        World.Pet kong = w.pet("kong");
        World.Pet bori = w.pet("bori");
        World.Pet dubu = w.pet("dubu");
        for (int ago : new int[] { 74, 44, 14 }) {
            schedule(kong, "미용·목욕", "위생미용", -ago, "14:00", null, null, "멍멍살롱, 발바닥·엉덩이만 짧게", 6);
        }
        schedule(bori, "미용·목욕", "목욕", -31, "16:00", null, null, "털갈이 시즌이라 집에서 목욕", 3);
        schedule(bori, "기타", "반려견 놀이터", -9, "11:00", null, null, "덕진공원 반려견 놀이터", 2);
        schedule(dubu, "병원", "신장 수치 재검사", 3, "10:30", "전북동물의료센터", "검사", "아침 굶기고 가기, 소변 받아 가기", 25);
        schedule(kong, "병원", "스케일링 상담", 6, "15:00", "온누리동물병원", "기타진료", "노령견 마취 가능한지 물어보기", 10);
        schedule(kong, "미용·목욕", "위생미용", 16, "14:00", null, null, "멍멍살롱", 2);
        schedule(bori, "병원", "체중 재측정", 12, "18:30", "덕진24시동물병원", "정기검진", "목표 12.5kg까지 얼마나 남았는지", 7);
        schedule(bori, "케어·투약", "구충제 먹이기", 4, null, null, null, null, 20);
    }

    /**
     * @param daysFromToday 음수면 지난 일정
     */
    private void schedule(World.Pet pet, String kind, String name, int daysFromToday, String time, String hospital,
            String ckupType, String memo, int registeredBefore) {
        LocalDate day = s.today().plusDays(daysFromToday);
        LocalDateTime at = time == null ? day.atStartOfDay() : day.atTime(LocalTime.parse(time));
        LocalDateTime registered = at.minusDays(registeredBefore).withHour(s.between(20, 22)).withMinute(s.between(0, 59));
        LocalDateTime regDt = registered.isAfter(s.now()) ? s.now().minusHours(1) : registered;
        s.store().add("SCHDL", r -> r.set("PET_SN", pet.sn()).set("SCHDL_KND_CD", c.of("HLTG19", kind))
                .set("SCHDL_SRC_CD", c.of("HLTG22", "직접 등록")).set("SCHDL_DT", at).set("SCHDL_NM", name).set("MEMO_CN", memo)
                .yn("HR_INPT_YN", time != null).set("HSPTL_NM", hospital)
                .set("CKUP_TYPE_CD", ckupType == null ? null : c.of("HLTG01", ckupType)).set("REG_DT", regDt).by(pet.ownerSn()));
    }

    // ---- 케어리스트

    /**
     * 기본 항목 11개(BSC_ARTCL_YN = 'Y')와 완료 이력. 고른 항목만 표시(INDCT_YN = 'Y')
     */
    private void careList(World.Pet pet, long mbrSn, Map<String, Double> chosen, int days) {
        LocalDate from = later(s.today().minusDays(days), pet.registeredAt().toLocalDate());
        for (Basic b : BASICS) {
            Double rate = chosen.get(b.name());
            Row care = s.store().add("CARE_ARTCL", r -> r.set("PET_SN", pet.sn())
                    .set("CARE_ZONE_CD", c.of("HLTG13", b.daily() ? "일상관리" : "정기관리"))
                    .set("CARE_CYCLE_CD", b.cycle() == null ? null : c.of("HLTG15", b.cycle())).set("CARE_ARTCL_NM", b.name())
                    .set("RPRS_ICON_ID", b.icon()).yn("BSC_ARTCL_YN", true).yn("INDCT_YN", rate != null)
                    .set("REG_DT", pet.registeredAt().plusDays(1).withHour(21)).by(mbrSn));
            LocalDate last = null;
            if (rate != null) {
                if (b.daily()) {
                    for (LocalDate d = from; !d.isAfter(s.today()); d = d.plusDays(1)) {
                        boolean today = d.equals(s.today());
                        if (!today && s.chance(rate) || today && s.now().getHour() >= 10 && s.chance(rate / 2)) {
                            completion(care.sn(), mbrSn, 1, d);
                            last = d;
                        }
                    }
                } else {
                    for (LocalDate d = from.plusDays(s.between(0, 3)); !d.isAfter(s.today().minusDays(1)); d = d.plusDays(b.days()
                            + (s.chance(1 - rate) ? s.between(1, 4) : 0))) {
                        completion(care.sn(), mbrSn, 1, d);
                        last = d;
                    }
                }
            }
            LocalDate lastDone = last;
            care.set("LAST_CMPTN_YMD", lastDone);
            if (!b.daily()) {
                LocalDate base = lastDone == null ? ((LocalDateTime) care.get("REG_DT")).toLocalDate() : lastDone;
                care.set("NXTM_PRNMNT_YMD", base.plusDays(b.days()));
            }
        }
    }

    private void extraCare(World.Pet pet, World.Member owner) {
        Map<String, Double> chosen = pet.dog() ? Map.of("양치", 0.3, "빗질", 0.5, "놀이", 0.6, "귀 청소", 0.6, "발톱 관리", 0.7)
                : Map.of("화장실 청소", 0.8, "빗질", 0.5, "놀이", 0.6);
        double scale = owner.activity().light() ? 0.6 : 1.0;
        Map<String, Double> scaled = new java.util.HashMap<>();
        chosen.forEach((k, v) -> scaled.put(k, v * scale));
        careList(pet, owner.sn(), scaled, 60);
        long daysSince = java.time.temporal.ChronoUnit.DAYS.between(pet.registeredAt().toLocalDate(), s.today());
        if (daysSince >= 40 && !owner.activity().light()) {
            int ago = s.between(10, (int) daysSince - 5);
            checkup(pet, ago, s.pick("10:30", "15:00", "17:30"), "예방접종", pet.dog() ? "덕진24시동물병원" : "온누리동물병원",
                    pet.dog() ? 45_000 : 40_000, pet.dog() ? "종합백신 추가접종. 특이 반응 없음." : "고양이 종합백신 추가접종. 특이 반응 없음.", null);
        }
    }

    /** 반복 예방 완료 이력을 항목 번호가 생긴 뒤에 붙인다 */
    private final java.util.List<Row> pendingPrevention = new java.util.ArrayList<>();

    private void completion(long careSn, long mbrSn, int times, LocalDate day) {
        LocalDateTime at = day.atTime(s.between(19, 22), s.between(0, 59));
        LocalDateTime when = at.isAfter(s.now()) ? s.now().minusMinutes(s.between(5, 60)) : at;
        Row row = s.store().add("CARE_CMPTN_HSTRY", r -> r.set("CARE_ARTCL_SN", careSn).set("MBR_SN", mbrSn).set("CMPTN_CYCL", times)
                .set("CMPTN_YMD", day).set("REG_DT", when).by(mbrSn));
        if (careSn < 0) {
            pendingPrevention.add(row);
        }
    }

    private void fixPending(long careSn) {
        pendingPrevention.forEach(row -> row.set("CARE_ARTCL_SN", careSn));
        pendingPrevention.clear();
    }

    // ---- 관찰 항목

    /**
     * 보리 귀 안쪽 붉은 기(어제 등록, 관찰중). 사진이 있는 관찰 주기는 시연에서 사람이 올린다
     */
    private void observation() {
        World.Pet bori = w.pet("bori");
        LocalDateTime at = s.now().minusDays(1).withHour(21).withMinute(18);
        Row item = s.store().add("OBSRV_ARTCL", r -> r.set("PET_SN", bori.sn()).set("OBSRV_REGN_CD", c.of("HLTG16", "머리"))
                .set("OBSRV_STTS_CD", c.of("HLTG18", "관찰중")).set("OBSRV_ARTCL_NM", "귀 안쪽 붉은 기")
                .set("OBSRV_REGN_DTL_NM", "오른쪽 귀 안쪽").set("REG_DT", at).by(bori.ownerSn()));
        s.store().add("OBSRV_ARTCL_STTS_HSTRY", r -> r.set("OBSRV_ARTCL_SN", item.sn()).set("MBR_SN", bori.ownerSn())
                .set("AFTR_STTS_CD", c.of("HLTG18", "관찰중")).set("REG_DT", at));
        w.observing.put(bori.sn(), true);
    }

    /** 주인공이 고른 건강기록 대시보드 항목(13개 모두 저장, HealthDashboardWidgetService.save와 같게 10 단위 순서) */
    private static final List<String> WIDGETS_SHOWN = List.of("체중", "검사 수치", "산책", "식사량", "음수량", "처방·투약", "케어리스트",
            "예방접종·정기검진", "검진기록", "알레르기·기저질환");
    private static final List<String> WIDGETS_HIDDEN = List.of("배변 상태", "지속 관찰", "첨부 자료");

    private void widgets(World.Member persona) {
        LocalDateTime at = s.now().minusDays(40).withHour(22).withMinute(5);
        int seq = 0;
        List<String> all = new java.util.ArrayList<>(WIDGETS_SHOWN);
        all.addAll(WIDGETS_HIDDEN);
        for (String name : all) {
            int order = seq += 10;
            s.store().add("MBR_DSHBD_WDGT", r -> r.set("MBR_SN", persona.sn()).set("DSHBD_WDGT_CD", c.of("HLTG25", name))
                    .yn("INDCT_YN", WIDGETS_SHOWN.contains(name)).set("SORT_SEQ", order).set("REG_DT", at));
        }
    }

    private People.PetPlan plan(World.Pet pet) {
        return People.everyExtra().stream().flatMap(p -> p.pets().stream()).filter(p -> p.key().equals(pet.key())).findFirst()
                .orElseThrow();
    }

    private static LocalDate later(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }
}
