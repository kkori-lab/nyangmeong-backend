package com.nyangmeong.seed.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.nyangmeong.seed.core.Codes;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.World;
import com.nyangmeong.seed.core.World.Activity;

/**
 * [가계부]
 * - 병원 지출은 건강기록의 병원 방문(World.visits)과 같은 날·같은 병원·같은 금액
 * - 주인공 : 3개월치 사료·간식·용품·미용·기타(펫보험). 반려동물 상태(관절·체중·신장)에 맞는 품목
 * - 엑스트라 : 사료와 가끔 간식·용품. 영수증(사진)은 넣지 않는다
 */
public class ExpenseSeeder implements Seeder {

    private Codes c;
    private Seed s;

    @Override
    public void run(Seed seed, World w) {
        s = seed;
        c = s.codes();
        for (World.Visit v : w.visits) {
            spend(v.ownerSn(), v.petSn(), "병원", v.amount(), v.hospital(), v.type() + " 진료비", v.date());
        }
        persona(w);
        for (World.Pet pet : w.pets) {
            World.Member owner = w.member(pet.ownerSn());
            if (!owner.persona()) {
                extra(pet, owner);
            }
        }
    }

    private void persona(World w) {
        long me = w.persona().sn();
        long kong = w.pet("kong").sn();
        long bori = w.pet("bori").sn();
        long dubu = w.pet("dubu").sn();
        every(me, kong, "사료", 34_000, "쿠팡", "시니어 소형견 사료 1.5kg", 88, 30);
        every(me, bori, "사료", 89_000, "스마트스토어", "체중관리 사료 7.5kg", 66, 45);
        every(me, dubu, "사료", 52_000, "전북동물의료센터", "신장 처방식 2kg", 86, 30);
        every(me, dubu, "용품", 18_900, "쿠팡", "벤토나이트 고양이 모래 6.35kg", 80, 28);
        every(me, kong, "용품", 39_000, "스마트스토어", "관절 영양제 60정", 79, 60);
        every(me, null, "용품", 22_000, "다이소", "배변패드 대형 50매", 75, 25);
        every(me, bori, "기타", 28_400, "펫보험", "보리 펫보험 월 보험료", 85, 30);
        for (int ago : new int[] { 74, 44, 14 }) {
            spend(me, kong, "미용", 25_000, "멍멍살롱", "위생미용", s.today().minusDays(ago));
        }
        String[][] treats = { { "보리", "저칼로리 오리 져키" }, { "콩이", "치석 관리 덴탈껌" }, { "두부", "신장 케어 습식 파우치" },
                { "보리", "동결건조 북어 트릿" } };
        for (int ago = 87; ago > 0; ago -= s.between(9, 15)) {
            String[] t = treats[ago % treats.length];
            long pet = "보리".equals(t[0]) ? bori : "콩이".equals(t[0]) ? kong : dubu;
            spend(me, pet, "간식", s.between(7, 16) * 1000, s.pick("쿠팡", "펫마트 전주점", "스마트스토어"), t[1], s.today().minusDays(ago));
        }
        spend(me, bori, "용품", 32_000, "펫마트 전주점", "노즈워크 매트", s.today().minusDays(53));
        spend(me, kong, "용품", 46_000, "스마트스토어", "미끄럼 방지 매트 4장", s.today().minusDays(80));
        spend(me, null, "용품", 15_800, "다이소", "털 제거 롤러 리필", s.today().minusDays(12));
    }

    private void extra(World.Pet pet, World.Member owner) {
        if (owner.activity() == Activity.NONE) {
            return;
        }
        long since = java.time.temporal.ChronoUnit.DAYS.between(pet.registeredAt().toLocalDate(), s.today());
        int firstAgo = (int) Math.max(0, Math.min(since, 60));
        int price = pet.dog() ? s.between(3, 7) * 10_000 : s.between(3, 6) * 10_000;
        every(owner.sn(), pet.sn(), "사료", price, s.pick("쿠팡", "스마트스토어", "펫마트 전주점"), pet.dog() ? "강아지 사료" : "고양이 사료",
                firstAgo, 30);
        if (!pet.dog()) {
            every(owner.sn(), pet.sn(), "용품", 16_900, "쿠팡", "고양이 모래", Math.max(0, firstAgo - 2), 30);
        }
        if (owner.activity() != Activity.LIGHT) {
            for (int ago = firstAgo - 3; ago > 0; ago -= s.between(10, 20)) {
                spend(owner.sn(), pet.sn(), "간식", s.between(5, 13) * 1000, s.pick("쿠팡", "펫마트 전주점"), pet.dog() ? "강아지 간식" : "츄르",
                        s.today().minusDays(ago));
            }
        }
    }

    /**
     * firstAgo일 전부터 everyDays마다
     */
    private void every(long mbrSn, Long petSn, String category, int amount, String place, String detail, int firstAgo,
            int everyDays) {
        for (int ago = firstAgo; ago >= 0; ago -= everyDays) {
            spend(mbrSn, petSn, category, amount, place, detail, s.today().minusDays(ago));
        }
    }

    private void spend(long mbrSn, Long petSn, String category, int amount, String place, String detail, LocalDate day) {
        LocalDateTime at = day.atTime(s.between(18, 23), s.between(0, 59));
        LocalDateTime when = at.isAfter(s.now()) ? s.now().minusMinutes(s.between(10, 90)) : at;
        s.store().add("EXPND", r -> r.set("PET_SN", petSn).set("MBR_SN", mbrSn).set("EXPND_CLSF_CD", c.of("MYPG05", category))
                .set("EXPND_INPT_MTH_CD", c.of("MYPG06", "직접입력")).set("EXPND_AMT", amount).set("EXPND_PLCUS_NM", place)
                .set("DTL_MTTR", detail).set("EXPND_YMD", day).set("REG_DT", when));
    }
}
