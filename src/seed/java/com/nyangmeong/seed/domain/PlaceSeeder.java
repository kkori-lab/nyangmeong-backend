package com.nyangmeong.seed.domain;

import java.time.LocalDateTime;
import java.util.List;

import com.nyangmeong.seed.core.Masters;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.SeedWarnings;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.World;
import com.nyangmeong.seed.core.World.Activity;

/**
 * [장소 즐겨찾기]
 * - 장소(PLC)는 남겨 둔 것을 쓴다. 전주 장소 중 옛 시드의 'n호점'이 아닌 곳
 * - 주인공은 집 근처 병원·미용실·용품점을, 엑스트라는 1~2곳을 즐겨찾기
 * - 옛 시드의 가짜 장소(n호점·fake- 카카오 ID)는 seedClean이 지운다
 */
public class PlaceSeeder implements Seeder {

    /** 덕진구 집 근처의 실제 장소(카카오 로컬에서 들어온 행) */
    private static final List<String> PERSONA_PLACES = List.of("전북동물병원", "송동물병원", "바버독", "견생냥품 24시 무인펫샵 전주혁신도시점");

    @Override
    public void run(Seed s, World w) {
        List<Masters.Place> jeonju = s.masters().placesIn("전주");
        if (jeonju.isEmpty()) {
            SeedWarnings.add("전주 장소가 없어 즐겨찾기를 넣지 않음");
            return;
        }
        World.Member persona = w.persona();
        for (String name : PERSONA_PLACES) {
            jeonju.stream().filter(p -> p.name().equals(name)).findFirst()
                    .ifPresentOrElse(p -> bookmark(s, persona.sn(), p.sn(), s.daysAgo(30, 140)),
                            () -> SeedWarnings.add("주인공 즐겨찾기 장소 '" + name + "'이 PLC에 없음"));
        }
        for (World.Member m : w.members) {
            if (m.persona() || m.activity() == Activity.NONE || m.activity() == Activity.LIGHT && s.chance(0.5)) {
                continue;
            }
            LocalDateTime from = m.joinedAt().isAfter(s.now().minusDays(60)) ? m.joinedAt() : s.now().minusDays(60);
            for (Masters.Place p : s.sample(jeonju, s.between(1, 2))) {
                bookmark(s, m.sn(), p.sn(), s.after(from));
            }
        }
    }

    private void bookmark(Seed s, long mbrSn, long plcSn, LocalDateTime at) {
        s.store().add("PLC_BKMK", r -> r.set("PLC_SN", plcSn).set("MBR_SN", mbrSn).set("REG_DT", at));
    }
}
