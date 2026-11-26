package com.nyangmeong.seed.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.seed.core.Codes;
import com.nyangmeong.seed.core.Masters;
import com.nyangmeong.seed.core.Row;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.World;

/**
 * [반려동물·양육관계]
 * - People의 계획대로 PET·BRUP_REL. 회원마다 첫 반려동물이 대표
 * - 공동양육 : 주인공의 보리를 보리아빠가 함께 키운다(초대 수락). 주인공이 두부이모에게 보낸 두부 초대는 대기중
 * - 주인공의 메인 바로가기(PET_MAIN_SHRTC)
 */
public class PetSeeder implements Seeder {

    private static final List<String> SHORTCUTS = List.of("건강 일지", "케어 캘린더", "케어미션", "검진기록", "케어리스트");

    private Codes c;

    @Override
    public void run(Seed s, World w) {
        c = s.codes();
        List<People.PersonPlan> plans = new ArrayList<>();
        plans.add(People.PERSONA);
        plans.addAll(People.EXTRAS);
        for (People.PersonPlan plan : plans) {
            World.Member owner = w.members.stream().filter(m -> m.key().equals(plan.key())).findFirst().orElse(null);
            if (owner == null) {
                continue;
            }
            boolean first = true;
            for (People.PetPlan petPlan : plan.pets()) {
                World.Pet pet = pet(s, w, owner, petPlan);
                relation(s, w, pet, owner.sn(), "주양육자", first, pet.registeredAt());
                first = false;
            }
        }
        joint(s, w);
        pending(s, w);
        shortcuts(s, w);
    }

    private World.Pet pet(Seed s, World w, World.Member owner, People.PetPlan p) {
        Masters m = s.masters();
        String petSe = c.of("PETG01", p.dog() ? "강아지" : "고양이");
        Long vrtySn = p.breed() == null ? null : m.variety(petSe, p.breed()).sn();
        LocalDateTime earliest = owner.joinedAt().plusMinutes(s.between(20, 300));
        LocalDateTime planned = s.today().minusDays(p.registeredDays()).atTime(s.between(9, 22), s.between(0, 59));
        LocalDateTime at = planned.isBefore(earliest) ? earliest : planned;
        LocalDateTime registeredAt = at.isAfter(s.now()) ? s.now().minusMinutes(s.between(5, 60)) : at;
        if (owner.withdrawn()) {
            long minutes = Math.max(1, java.time.Duration.between(owner.joinedAt(), owner.leftAt()).toMinutes());
            registeredAt = owner.joinedAt().plusSeconds(30 + s.between(0, (int) Math.min(minutes * 60 / 2, 86_400)));
        } else if (registeredAt.isBefore(owner.joinedAt())) {
            registeredAt = owner.joinedAt().plusMinutes(1);
        }
        LocalDateTime petRegisteredAt = registeredAt;
        LocalDate birth = s.today().minusMonths(p.ageMonths()).withDayOfMonth(s.between(1, 28));
        double weight = p.weightNow();
        double food = weight * (p.dog() ? 18 : 14);
        Row row = s.store().add("PET", r -> {
            r.set("VRTY_SN", vrtySn).set("PET_SE_CD", petSe).set("PET_GNDR_CD", c.of("PETG02", p.male() ? "수컷" : "암컷"))
                    .set("COLR_CD", c.of("PETG03", p.color())).set("PET_NM", p.name()).set("VRTY_DRCT_INPT_NM", p.breedInput())
                    .set("CHRC_CN", p.trait()).set("ALGY_CN", p.allergy()).set("DSS_CN", p.disease()).set("PET_BRDT", birth)
                    .set("PET_BDWG", s.decimal(weight, 1)).set("GOAL_BDWG", p.goal() == null ? null : s.decimal(p.goal(), 1))
                    .set("MIN_BDWG", s.decimal(weight * 0.9, 1)).set("MAX_BDWG", s.decimal(weight * 1.1, 1))
                    .set("MIN_MEAL_QTY", s.decimal(Math.round(food * 0.9), 0)).set("MAX_MEAL_QTY", s.decimal(Math.round(food * 1.1), 0))
                    .set("MIN_WATR_QTY", s.decimal(Math.round(weight * 40), 0)).set("MAX_WATR_QTY", s.decimal(Math.round(weight * 60), 0))
                    .yn("NUTR_YN", p.neutered()).set("REG_DT", petRegisteredAt).by(owner.sn());
        });
        World.Pet pet = new World.Pet(row.sn(), p.key(), owner.sn(), p.dog(), p.name(), vrtySn,
                p.breed() == null ? p.breedInput() : p.breed(), birth, BigDecimal.valueOf(weight), petRegisteredAt);
        w.pets.add(pet);
        return pet;
    }

    private Row relation(Seed s, World w, World.Pet pet, long mbrSn, String role, boolean representative, LocalDateTime at) {
        boolean rprs = representative || !w.representativePet.containsKey(mbrSn);
        Row rel = s.store().add("BRUP_REL", r -> r.set("PET_SN", pet.sn()).set("MBR_SN", mbrSn)
                .set("BRUP_ROLE_CD", c.of("PETG05", role)).yn("RPRS_PET_YN", rprs).set("REG_DT", at).by(mbrSn));
        if (rprs) {
            w.representativePet.put(mbrSn, pet.sn());
        }
        w.caredPets.computeIfAbsent(mbrSn, k -> new ArrayList<>()).add(pet.sn());
        return rel;
    }

    /**
     * 보리아빠가 보리의 공동양육자(주인공이 초대, 수락)
     */
    private void joint(Seed s, World w) {
        World.Member persona = w.persona();
        World.Member dad = w.member("boriDad");
        World.Pet bori = w.pet("bori");
        LocalDateTime sentAt = bori.registeredAt().plusDays(3).withHour(21).withMinute(12);
        LocalDateTime acceptedAt = sentAt.plusMinutes(47);
        Row invitation = invitation(s, persona, dad.sn(), dad.email(), "수락", sentAt, acceptedAt);
        s.store().add("INVTN_TRGT_PET", r -> r.set("JNT_BRUP_INVTN_SN", invitation.sn()).set("PET_SN", bori.sn())
                .yn("ACPT_YN", true).set("REG_DT", sentAt));
        invitationHistory(s, invitation.sn(), persona.sn(), null, "대기중", sentAt);
        invitationHistory(s, invitation.sn(), dad.sn(), "대기중", "수락", acceptedAt);
        Row rel = relation(s, w, bori, dad.sn(), "공동양육자", true, acceptedAt);
        s.store().add("BRUP_LINK_STTS_HSTRY", r -> r.set("BRUP_REL_SN", rel.sn()).set("MBR_SN", dad.sn())
                .set("LINK_CHG_TYPE_CD", c.of("PETG07", "초대 수락 연동")).set("REG_DT", acceptedAt));
    }

    /**
     * 주인공이 두부이모에게 두부 공동양육 초대(대기중) : 두부이모 알림함에 초대가 보인다
     */
    private void pending(Seed s, World w) {
        World.Member persona = w.persona();
        World.Member aunt = w.member("dubuAunt");
        World.Pet dubu = w.pet("dubu");
        LocalDateTime sentAt = s.now().minusDays(1).withHour(20).withMinute(41);
        Row invitation = invitation(s, persona, null, aunt.email(), "대기중", sentAt, null);
        s.store().add("INVTN_TRGT_PET", r -> r.set("JNT_BRUP_INVTN_SN", invitation.sn()).set("PET_SN", dubu.sn())
                .set("REG_DT", sentAt));
        invitationHistory(s, invitation.sn(), persona.sn(), null, "대기중", sentAt);
        w.pendingInvitation = new World.Invitation(invitation.sn(), aunt.sn(), dubu.sn(), sentAt);
    }

    private Row invitation(Seed s, World.Member sender, Long receiverSn, String email, String status, LocalDateTime sentAt,
            LocalDateTime respondedAt) {
        FieldCryptoService crypto = s.crypto();
        int days = s.masters().intSetting("INVTN_VLD_DAY_CNT");
        return s.store().add("JNT_BRUP_INVTN", r -> r.set("SNDR_MBR_SN", sender.sn()).set("RCVR_MBR_SN", receiverSn)
                .set("INVTN_STTS_CD", c.of("PETG06", status)).set("INVTN_TRGT_EML_ENC", crypto.encrypt(crypto.normalizeEmail(email)))
                .set("INVTN_TRGT_EML_HMAC", crypto.emailHmac(email)).set("INVTN_EXPRY_DT", sentAt.plusDays(days))
                .set("ACPT_RFSL_RSPNS_DT", respondedAt).set("INVTN_TKN_HASH", s.hex64()).set("REG_DT", sentAt)
                .by(sender.sn()));
    }

    private void invitationHistory(Seed s, long invitationSn, Long mbrSn, String before, String after, LocalDateTime at) {
        s.store().add("JNT_BRUP_INVTN_STTS_HSTRY", r -> r.set("JNT_BRUP_INVTN_SN", invitationSn).set("MBR_SN", mbrSn)
                .set("BFR_STTS_CD", before == null ? null : c.of("PETG06", before)).set("AFTR_STTS_CD", c.of("PETG06", after))
                .set("REG_DT", at));
    }

    private void shortcuts(Seed s, World w) {
        World.Member persona = w.persona();
        for (World.Pet pet : w.petsOf(persona.sn())) {
            int seq = 0;
            for (String name : SHORTCUTS) {
                int order = seq += 10;
                s.store().add("PET_MAIN_SHRTC", r -> r.set("MBR_SN", persona.sn()).set("PET_SN", pet.sn())
                        .set("SHRTC_CD", c.of("MANG02", name)).yn("INDCT_YN", true).set("SORT_SEQ", order)
                        .set("REG_DT", pet.registeredAt().plusMinutes(30)));
            }
        }
    }
}
