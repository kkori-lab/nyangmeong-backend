package com.nyangmeong.seed.core;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [Seeder 사이에서 넘기는 값]
 * - 앞 Seeder가 만든 회원·반려동물·게시글을 뒤 Seeder가 쓴다
 */
public final class World {

    /** 활동량 : 글·댓글·기록을 얼마나 남기는지 */
    public enum Activity {
        HEAVY, NORMAL, LIGHT, NONE
    }

    /**
     * @param key      People의 이름표(주인공은 "persona")
     * @param sggNm    사는 시군구
     * @param emdNm    사는 읍면동
     */
    /**
     * @param leftAt 탈퇴한 회원이면 탈퇴일시(그 뒤의 활동은 만들지 않는다)
     */
    public record Member(long sn, String key, String nickname, String email, LocalDateTime joinedAt, Activity activity,
            String sggNm, String emdNm, LocalDateTime leftAt) {

        public boolean withdrawn() {
            return leftAt != null;
        }

        public boolean persona() {
            return "persona".equals(key);
        }
    }

    public record Pet(long sn, String key, long ownerSn, boolean dog, String name, Long vrtySn, String breed,
            LocalDate birth, BigDecimal weight, LocalDateTime registeredAt) {
    }

    public record Post(long sn, long mbrSn, String bbs, LocalDateTime at) {
    }

    public record Invitation(long sn, long receiverSn, long petSn, LocalDateTime sentAt) {
    }

    /** 병원 방문(가계부 병원 지출과 검진기록 포인트가 쓴다) */
    public record Visit(long petSn, long ownerSn, LocalDate date, String hospital, String type, int amount) {
    }

    /** 인앱 알림으로 남길 일(NotificationSeeder가 쓴다) */
    public record Alarm(long mbrSn, String type, String title, String content, String path, LocalDateTime at) {
    }

    public final List<Member> members = new ArrayList<>();
    public final List<Alarm> alarms = new ArrayList<>();
    public final List<Visit> visits = new ArrayList<>();
    public final List<Pet> pets = new ArrayList<>();
    public final List<Post> posts = new ArrayList<>();
    /** 회원 → 양육 중인 반려동물(주양육·공동양육) */
    public final Map<Long, List<Long>> caredPets = new HashMap<>();
    /** 회원 → 대표 반려동물 */
    public final Map<Long, Long> representativePet = new HashMap<>();
    /** 반려동물 → 관찰 중인 항목이 있는지 */
    public final Map<Long, Boolean> observing = new HashMap<>();
    /** 주인공이 보낸 대기 중인 공동양육 초대(알림이 쓴다) */
    public Invitation pendingInvitation;

    public Member member(String key) {
        return members.stream().filter(m -> m.key().equals(key)).findFirst()
                .orElseThrow(() -> new IllegalStateException("회원 " + key + "이 없습니다"));
    }

    public Member member(long sn) {
        return members.stream().filter(m -> m.sn() == sn).findFirst()
                .orElseThrow(() -> new IllegalStateException("회원 " + sn + "번이 없습니다"));
    }

    public Member persona() {
        return member("persona");
    }

    public Pet pet(String key) {
        return pets.stream().filter(p -> p.key().equals(key)).findFirst()
                .orElseThrow(() -> new IllegalStateException("반려동물 " + key + "이 없습니다"));
    }

    public Pet pet(long sn) {
        return pets.stream().filter(p -> p.sn() == sn).findFirst()
                .orElseThrow(() -> new IllegalStateException("반려동물 " + sn + "번이 없습니다"));
    }

    public List<Pet> petsOf(long ownerSn) {
        return pets.stream().filter(p -> p.ownerSn() == ownerSn).toList();
    }

    /** 커뮤니티에 글·댓글을 남기는 회원(활동량 NONE 제외) */
    public List<Member> writers() {
        return members.stream().filter(m -> m.activity() != Activity.NONE).toList();
    }
}
