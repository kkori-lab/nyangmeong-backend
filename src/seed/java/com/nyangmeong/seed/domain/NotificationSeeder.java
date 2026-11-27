package com.nyangmeong.seed.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.nyangmeong.seed.core.Codes;
import com.nyangmeong.seed.core.Row;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.World;

/**
 * [인앱 알림]
 * - 다른 Seeder가 남긴 일(World.alarms : 댓글·채팅·문의 답변)과 주인공의 케어 일정 사전 알림, 지출 한도 초과, 두부이모의 공동양육 초대
 * - 문구·이동 경로는 각 알림 서비스와 같다. 자동숨김일시 = 등록일시 + SYS_STNG.NOTI_HIDE_CRTR_DAY_CNT
 * - 사흘 넘은 알림은 읽음, 그 안의 알림은 안 읽음
 */
public class NotificationSeeder implements Seeder {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private Codes c;
    private Seed s;
    private int hideDays;

    @Override
    public void run(Seed seed, World w) {
        s = seed;
        c = s.codes();
        hideDays = s.masters().intSetting("NOTI_HIDE_CRTR_DAY_CNT");
        for (World.Alarm a : w.alarms) {
            if (!a.at().isBefore(s.now().minusDays(30))) {
                alarm(a.mbrSn(), a.type(), a.title(), a.content(), a.path(), a.at());
            }
        }
        scheduleAlarms(w);
        expenseLimit(w);
        invitation(w);
    }

    /**
     * 주인공의 다가오는 병원 일정 : 정해 둔 사전 알림(D-3·D-1, 오전 9시)
     */
    private void scheduleAlarms(World w) {
        long me = w.persona().sn();
        String hospital = c.of("HLTG19", "병원");
        for (Row r : s.store().rows("SCHDL")) {
            World.Pet pet = w.pet((Long) r.get("PET_SN"));
            if (pet.ownerSn() != me || !hospital.equals(r.get("SCHDL_KND_CD"))) {
                continue;
            }
            LocalDate day = ((LocalDateTime) r.get("SCHDL_DT")).toLocalDate();
            for (int before : new int[] { 3, 1 }) {
                LocalDateTime at = day.minusDays(before).atTime(9, 0);
                if (at.isAfter(s.now()) || at.isBefore(s.now().minusDays(7)) || !at.isAfter((LocalDateTime) r.get("REG_DT"))) {
                    continue;
                }
                String content = day.format(YMD) + (r.get("HSPTL_NM") == null ? "" : " · " + r.get("HSPTL_NM"));
                alarm(me, "케어 일정", "[케어 일정] " + pet.name() + " " + r.get("SCHDL_NM") + " 일정이 D-" + before + " 남았어요", content,
                        "/health/calendar", at);
            }
        }
    }

    /**
     * 이번 달 지출이 한도(400,000원)를 넘은 날 알림
     */
    private void expenseLimit(World w) {
        long me = w.persona().sn();
        int limit = 400_000;
        LocalDate first = s.today().withDayOfMonth(1);
        int sum = 0;
        java.util.List<Row> rows = s.store().rows("EXPND").stream()
                .filter(r -> ((Long) r.get("MBR_SN")) == me && !((LocalDate) r.get("EXPND_YMD")).isBefore(first))
                .sorted(java.util.Comparator.comparing(r -> (LocalDateTime) r.get("REG_DT"))).toList();
        for (Row r : rows) {
            int before = sum;
            sum += ((Number) r.get("EXPND_AMT")).intValue();
            if (before <= limit && sum > limit) {
                alarm(me, "가계부", "이번 달 지출이 한도를 넘었어요", String.format("%,d원 / %,d원", sum, limit), "/mypage/expenses",
                        (LocalDateTime) r.get("REG_DT"));
            }
        }
    }

    private void invitation(World w) {
        World.Invitation inv = w.pendingInvitation;
        if (inv == null) {
            return;
        }
        alarm(inv.receiverSn(), "공동양육", w.pet(inv.petSn()).name() + "의 공동양육자가 되어주세요", w.persona().nickname() + "님이 초대했어요",
                "/mypage/invitations/" + inv.sn(), inv.sentAt());
    }

    private void alarm(long mbrSn, String type, String title, String content, String path, LocalDateTime at) {
        boolean read = at.isBefore(s.now().minusDays(3));
        LocalDateTime readAt = read ? s.soonAfter(at, 60 * 12) : null;
        s.store().add("INAPP_NOTI", r -> r.set("MBR_SN", mbrSn).set("NOTI_TYPE_CD", c.of("AIMG02", type)).set("NOTI_TTL", title)
                .set("NOTI_CN", content).set("MVMN_PATH", path).set("PRSL_DT", readAt).set("AUTO_HIDE_DT", at.plusDays(hideDays))
                .set("REG_DT", at));
    }
}
