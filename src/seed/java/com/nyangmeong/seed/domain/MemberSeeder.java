package com.nyangmeong.seed.domain;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.seed.SeedException;
import com.nyangmeong.seed.core.Codes;
import com.nyangmeong.seed.core.Masters;
import com.nyangmeong.seed.core.Row;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.SeedWarnings;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.World;
import com.nyangmeong.seed.core.World.Activity;

/**
 * [회원]
 * - 주인공(회원 1) : 계정은 그대로 두고 닉네임을 바꾸고 비밀번호를 app.seed.password로 맞춘다
 * - 남긴 엑스트라 : 계정을 건드리지 않는다
 * - 새 엑스트라 : MBR·MBR_STTS_HSTRY·MBR_TRMS_AGRE. 메일은 주인공과 같은 nyangmeong.dev, 이메일 알림은 끈다
 * - 꼭 있어야 하는 계정(app.seed.fixed-accounts) : 남긴 회원 중에 없으면 멈추고, 있으면 비밀번호를 app.seed.fixed-password로 맞춘다
 */
public class MemberSeeder implements Seeder {

    /** 새 회원 메일 도메인(주인공 tester@nyangmeong.dev와 같은 시드 전용 도메인, 사람 2026-11-26) */
    static final String EMAIL_DOMAIN = "@nyangmeong.dev";

    @Override
    public void run(Seed s, World w) {
        Codes c = s.codes();
        Masters m = s.masters();
        FieldCryptoService crypto = s.crypto();
        Masters.KeptMember admin = m.members().stream()
                .filter(k -> "관리자".equals(c.name("USRG02", k.authorityCd())) && "정상".equals(c.name("USRG01", k.statusCd())))
                .findFirst().orElse(null);
        w.adminSn = admin == null ? null : admin.sn();
        Set<String> nicknames = new HashSet<>();
        m.members().forEach(k -> nicknames.add(k.nickname()));

        Masters.KeptMember persona = m.member(People.PERSONA.keptSn());
        // 다시 넣을 때는 주인공이 이미 그 닉네임을 쓰고 있다
        nicknames.remove(persona.nickname());
        if (!nicknames.add(People.PERSONA.nickname())) {
            throw new SeedException("주인공 닉네임 '" + People.PERSONA.nickname() + "'을 이미 다른 회원이 씁니다");
        }
        Map<String, Object> personaUpdate = new HashMap<>();
        personaUpdate.put("MBR_NCNM", People.PERSONA.nickname());
        personaUpdate.put("PSWD_HASH", s.passwordHash());
        personaUpdate.put("NCNM_STNG_CMPTN_YN", "Y");
        personaUpdate.put("TUTRL_VIEW_YN", "Y");
        personaUpdate.put("MM_EXPND_LIM_AMT", 400_000);
        personaUpdate.put("EXPND_LIM_NOTI_RCPTN_YN", "Y");
        personaUpdate.put("EML_NOTI_RCPTN_YN", "N");
        personaUpdate.put("D1_BFHD_NOTI_YN", "Y");
        personaUpdate.put("D3_BFHD_NOTI_YN", "Y");
        personaUpdate.put("LAST_LGN_DT", s.now().minusHours(2));
        personaUpdate.put("MDFR_ID", String.valueOf(persona.sn()));
        s.store().update("MBR", persona.sn(), personaUpdate);
        w.members.add(new World.Member(persona.sn(), People.PERSONA.key(), People.PERSONA.nickname(),
                crypto.decrypt(persona.emailEnc()), persona.joinedAt(), People.PERSONA.activity(), People.SGG,
                People.PERSONA.emdNm(), null, null));

        fixedAccounts(s, m, crypto);

        for (People.PersonPlan plan : People.everyExtra()) {
            if (plan.keptSn() != null) {
                if (!m.hasMember(plan.keptSn())) {
                    SeedWarnings.add("남긴 회원 " + plan.keptSn() + "번이 없어 엑스트라 " + plan.key() + "를 건너뜀");
                    continue;
                }
                Masters.KeptMember kept = m.member(plan.keptSn());
                String status = c.name("USRG01", kept.statusCd());
                boolean withdrawn = "탈퇴".equals(status);
                if (!"정상".equals(status) && !withdrawn) {
                    SeedWarnings.add("남긴 회원 " + kept.sn() + "번이 " + status + " 회원이라 데이터를 넣지 않음");
                    continue;
                }
                // 탈퇴한 테스트 계정은 탈퇴 전에 남긴 것(반려동물 등록·댓글)만 만든다
                LocalDateTime leftAt = withdrawn ? (kept.withdrawnAt() == null ? kept.joinedAt().plusMinutes(5) : kept.withdrawnAt()) : null;
                w.members.add(new World.Member(kept.sn(), plan.key(), kept.nickname(), crypto.decrypt(kept.emailEnc()),
                        kept.joinedAt(), withdrawn ? Activity.NONE : plan.activity(), People.SGG, plan.emdNm(), leftAt, null));
                continue;
            }
            if (!nicknames.add(plan.nickname())) {
                throw new SeedException("엑스트라 닉네임 '" + plan.nickname() + "'을 이미 다른 회원이 씁니다");
            }
            newMember(s, c, crypto, w, plan);
        }
    }

    private void fixedAccounts(Seed s, Masters m, FieldCryptoService crypto) {
        for (String email : s.fixedAccounts()) {
            String normalized = crypto.normalizeEmail(email);
            Masters.KeptMember member = m.members().stream()
                    .filter(k -> normalized.equals(crypto.normalizeEmail(crypto.decrypt(k.emailEnc())))).findFirst()
                    .orElseThrow(() -> new SeedException("꼭 있어야 하는 계정 " + email + "이 MBR에 없습니다. seedClean이 남기는 회원인지 확인해 주세요"));
            s.store().update("MBR", member.sn(), Map.of("PSWD_HASH", s.fixedPasswordHash()));
            System.out.println("[시드] 꼭 있어야 하는 계정 " + member.sn() + "번의 비밀번호를 app.seed.fixed-password로 맞춥니다");
        }
    }

    private void newMember(Seed s, Codes c, FieldCryptoService crypto, World w, People.PersonPlan plan) {
        ExtraPeople.Meta meta = ExtraPeople.META.get(plan.key());
        ExtraPeople.Status status = meta == null ? ExtraPeople.Status.NORMAL : meta.status();
        String email = plan.emailLocal() + EMAIL_DOMAIN;
        LocalDateTime joinedAt = s.today().minusDays(plan.joinedDays()).atTime(s.between(9, 23), s.between(0, 59));
        // 휴면 시작·정지·탈퇴한 시각(그 뒤로 이 회원의 활동은 없다)
        LocalDateTime changedAt = meta == null ? null : s.now().minusDays(meta.daysAgo()).minusMinutes(s.between(0, 300));
        boolean suspended = status == ExtraPeople.Status.SUSPENDED || status == ExtraPeople.Status.BANNED;
        boolean withdrawn = status == ExtraPeople.Status.WITHDRAWN;
        LocalDateTime leftAt = withdrawn ? changedAt : null;
        LocalDateTime inactiveFrom = changedAt != null && !withdrawn ? changedAt : null;
        LocalDateTime lastLogin = changedAt != null ? changedAt.minusMinutes(s.between(5, 600)) : switch (plan.activity()) {
        case HEAVY -> s.now().minusHours(s.between(1, 20));
        case NORMAL -> s.now().minusDays(s.between(1, 3));
        case LURKER -> s.now().minusHours(s.between(2, 40));
        default -> s.now().minusDays(s.between(2, 9));
        };
        String statusName = withdrawn ? "탈퇴" : suspended ? "정지" : "정상";
        // 기간 정지 : 정지한 날 + 7일 0시(영구 정지는 비워 둔다)
        LocalDateTime suspendEnd = status == ExtraPeople.Status.SUSPENDED ? changedAt.toLocalDate().plusDays(7).atStartOfDay() : null;
        Row row = s.store().add("MBR", r -> {
            r.set("MBR_STTS_CD", c.of("USRG01", statusName)).set("MBR_AUTHRT_CD", c.of("USRG02", "일반회원"))
                    .set("JOIN_PATH_CD", c.of("USRG03", "이메일")).set("ACNT_EML_ENC", crypto.encrypt(crypto.normalizeEmail(email)))
                    .set("ACNT_EML_HMAC", crypto.emailHmac(email)).set("PSWD_HASH", s.passwordHash()).set("MBR_NCNM", plan.nickname())
                    .yn("NCNM_STNG_CMPTN_YN", true).yn("TUTRL_VIEW_YN", true).yn("EML_NOTI_RCPTN_YN", false)
                    .set("LAST_LGN_DT", lastLogin).set("REG_DT", joinedAt);
            if (suspendEnd != null) {
                r.set("SUSPN_END_DT", suspendEnd);
            }
            if (leftAt != null) {
                r.set("WHDWL_DT", leftAt);
            }
        });
        s.store().add("MBR_STTS_HSTRY", r -> r.set("MBR_SN", row.sn()).set("AFTR_STTS_CD", c.of("USRG01", "정상"))
                .set("CHG_RSN", "회원가입").set("REG_DT", joinedAt));
        if (suspended) {
            s.store().add("MBR_STTS_HSTRY", r -> {
                r.set("MBR_SN", row.sn()).set("BFR_STTS_CD", c.of("USRG01", "정상")).set("AFTR_STTS_CD", c.of("USRG01", "정지"))
                        .set("CHG_RSN", meta.reason()).set("REG_DT", changedAt);
                if (w.adminSn != null) {
                    r.set("PRCS_MBR_SN", w.adminSn);
                }
            });
        }
        if (withdrawn) {
            s.store().add("MBR_STTS_HSTRY", r -> r.set("MBR_SN", row.sn()).set("BFR_STTS_CD", c.of("USRG01", "정상"))
                    .set("AFTR_STTS_CD", c.of("USRG01", "탈퇴")).set("CHG_RSN", "회원 탈퇴").set("REG_DT", changedAt));
        }
        for (Masters.Terms terms : s.masters().terms()) {
            boolean agreed = terms.essential() || s.chance(0.35);
            s.store().add("MBR_TRMS_AGRE", r -> r.set("MBR_SN", row.sn()).set("TRMS_SN", terms.sn()).set("AGRE_DT", joinedAt)
                    .yn("AGRE_YN", agreed).set("REG_DT", joinedAt).by(row.sn()));
        }
        w.members.add(new World.Member(row.sn(), plan.key(), plan.nickname(), email, joinedAt, plan.activity(), People.SGG,
                plan.emdNm(), leftAt, inactiveFrom));
    }

    static boolean writes(World.Member m) {
        return m.activity() != Activity.NONE;
    }
}
