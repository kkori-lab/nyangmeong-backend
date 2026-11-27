package com.nyangmeong.seed.domain;

import java.util.List;

import com.nyangmeong.seed.core.World.Activity;

/**
 * [시연 페르소나]
 * - 주인공(회원 1)과 엑스트라. 남긴 회원(keptSn)은 계정을 그대로 두고 반려동물·활동만 넣는다
 * - 나이·품종·체중·질환이 건강기록·가계부·글과 서로 맞게 정한다(HealthSeeder·ExpenseSeeder·CommunitySeeder가 key로 찾는다)
 * - 지역은 팀 실제 데이터와 같은 전주시 동네
 */
final class People {

    /**
     * @param ageMonths      오늘 기준 나이(개월)
     * @param weightStart    90일 전(또는 등록 때) 체중
     * @param weightNow      오늘 체중
     * @param registeredDays 며칠 전에 등록했는지(가입일보다 앞서면 가입 직후로 당긴다)
     */
    record PetPlan(String key, String name, boolean dog, String breed, String breedInput, boolean male, int ageMonths,
            double weightStart, double weightNow, Double goal, boolean neutered, String color, String trait, String allergy,
            String disease, int registeredDays) {
    }

    /**
     * @param keptSn     남긴 회원 번호. null이면 새 회원
     * @param nickname   새 회원·주인공의 닉네임(남긴 엑스트라는 null, 계정 그대로)
     * @param joinedDays 새 회원이 며칠 전에 가입했는지
     */
    record PersonPlan(String key, Long keptSn, String nickname, String emailLocal, int joinedDays, Activity activity,
            String emdNm, List<PetPlan> pets) {
    }

    static final String SGG = "전주시";

    static final PersonPlan PERSONA = new PersonPlan("persona", 1L, "콩이네", null, 0, Activity.HEAVY, "덕진동",
            List.of(new PetPlan("kong", "콩이", true, "말티즈", null, true, 11 * 12 + 2, 3.5, 3.4, null, true, "흰색",
                    "겁이 많지만 안기는 걸 제일 좋아해요. 산책은 짧게 자주 가요.", null, "슬개골 탈구 2기(왼쪽 뒷다리)", 150),
                    new PetPlan("bori", "보리", true, "웰시 코기", null, false, 3 * 12 + 4, 14.2, 13.3, 12.5, true, "황색",
                            "먹을 것만 보면 눈이 반짝여요. 하루 산책 두 번은 꼭 해야 해요.", null, null, 150),
                    new PetPlan("dubu", "두부", false, "코리안 숏헤어", null, true, 13 * 12 + 1, 4.6, 4.4, null, true, "턱시도",
                            "조용하고 사람 무릎을 좋아해요. 낯선 사람이 오면 숨어요.", "닭고기 알레르기", "만성 신장병 2기(2026년 9월 진단)",
                            150)));

    static final List<PersonPlan> EXTRAS = List.of(
            new PersonPlan("boriDad", null, "보리아빠", "boridad", 205, Activity.NORMAL, "덕진동", List.of()),
            new PersonPlan("dubuAunt", null, "두부이모", "dubu", 27, Activity.LIGHT, "서신동",
                    List.of(new PetPlan("hotteok", "호떡", false, "코리안 숏헤어", null, false, 2 * 12 + 3, 3.8, 3.9, null, true,
                            "치즈", "장난감 쥐를 물어 오는 게 취미예요.", null, null, 26))),
            new PersonPlan("cheese", null, "치즈집사", "cheese", 165, Activity.HEAVY, "금암동",
                    List.of(new PetPlan("cheeseCat", "치즈", false, "코리안 숏헤어", null, true, 4 * 12 + 6, 5.9, 5.6, 5.2, true,
                            "치즈", "밥 시간 10분 전부터 울어요.", null, null, 160),
                            new PetPlan("mango", "망고", false, "노르웨이 숲", null, false, 3 * 12, 4.7, 4.8, null, true, "삼색",
                                    "털이 많이 빠져서 빗질을 매일 해요.", null, null, 160))),
            new PersonPlan("songcheon", null, "단추아빠", "danchu", 190, Activity.HEAVY, "송천1동",
                    List.of(new PetPlan("danchu", "단추", true, "시바견", null, true, 5 * 12 + 2, 9.8, 9.6, null, true, "황색",
                            "산책 중에 멈춰서 안 움직이는 시바 특유의 고집이 있어요.", null, null, 185))),
            new PersonPlan("inhuPom", null, "솜이맘", "somi", 115, Activity.NORMAL, "인후1동",
                    List.of(new PetPlan("somi", "솜이", true, "포메라니안", null, false, 9 * 12 + 5, 2.7, 2.6, null, true, "크림",
                            "나이가 들어서 잠이 많아졌어요. 계단은 안아서 올라가요.", null, "기관지 협착(기침이 잦아요)", 110))),
            new PersonPlan("seosin", null, "구름이네", "gureum", 75, Activity.LIGHT, "서신동",
                    List.of(new PetPlan("gureum", "구름", false, "랙돌", null, true, 14, 4.1, 4.6, null, false, "포인트",
                            "아직 아기라 밤마다 우다다를 해요.", null, null, 70))),
            new PersonPlan("shy", 6L, null, null, 0, Activity.NORMAL, "진북동",
                    List.of(new PetPlan("nabi", "나비", false, "러시안 블루", null, false, 5 * 12 + 1, 4.1, 4.0, null, true, "회색",
                            "창밖 새 구경을 하루 종일 해요.", null, null, 120))),
            new PersonPlan("mallang", 93L, null, null, 0, Activity.NORMAL, "우아1동",
                    List.of(new PetPlan("tteok", "떡이", true, "비숑 프리제", null, true, 2 * 12 + 2, 5.6, 5.9, null, true, "흰색",
                            "미용하고 오면 한동안 삐져 있어요.", "소고기 알레르기", null, 85))),
            new PersonPlan("t201", 201L, null, null, 0, Activity.LIGHT, "금암동",
                    List.of(new PetPlan("cookie", "쿠키", true, "푸들", null, false, 4 * 12, 3.9, 3.9, null, true, "갈색",
                            "공놀이를 시작하면 멈추지 않아요.", null, null, 19))),
            new PersonPlan("t202", 202L, null, null, 0, Activity.LIGHT, "진북동",
                    List.of(new PetPlan("tteokbokki", "떡볶이", false, "코리안 숏헤어", null, true, 3 * 12, 5.1, 5.2, null, true, "고등어",
                            "츄르 봉지 소리만 나면 달려와요.", null, null, 19))),
            new PersonPlan("t205", 205L, null, null, 0, Activity.LIGHT, "송천1동",
                    List.of(new PetPlan("happy", "해피", true, "진돗개", null, true, 6 * 12, 17.6, 17.4, null, true, "흰색",
                            "마당 있는 집에서 지내다 아파트로 이사 왔어요.", null, null, 14))),
            new PersonPlan("t206", 206L, null, null, 0, Activity.LIGHT, "인후1동",
                    List.of(new PetPlan("momo", "모모", false, "스코티시 폴드", null, false, 2 * 12, 3.6, 3.6, null, true, "크림",
                            "관절이 약한 품종이라 높은 곳에 못 올라가게 해요.", null, null, 13))),
            new PersonPlan("t207", 207L, null, null, 0, Activity.NORMAL, "덕진동",
                    List.of(new PetPlan("choco", "초코", true, "닥스훈트", null, true, 7 * 12 + 3, 6.9, 6.8, 6.3, true, "갈색",
                            "허리가 길어서 소파 계단을 놓아 줬어요.", null, "디스크 수술 이력(2024년)", 10))),
            new PersonPlan("t209", 209L, null, null, 0, Activity.NORMAL, "우아1동",
                    List.of(new PetPlan("nureong", "누렁이", true, null, "믹스견", false, 9 * 12, 12.3, 12.0, null, true, "황색",
                            "보호소에서 데려온 지 5년 됐어요. 사람을 정말 좋아해요.", null, null, 6))),
            new PersonPlan("t212", 212L, null, null, 0, Activity.LIGHT, "서신동",
                    List.of(new PetPlan("lulu", "루루", false, "페르시안", null, false, 8 * 12, 4.4, 4.4, null, true, "흰색",
                            "눈물이 많아서 매일 닦아 줘요.", null, null, 3))),
            new PersonPlan("t213", 213L, null, null, 0, Activity.LIGHT, "금암동",
                    List.of(new PetPlan("podo", "포도", false, "브리티시 숏헤어", null, true, 13, 3.9, 4.1, null, false, "회색",
                            "새로 온 아기 고양이예요.", null, null, 2))),
            new PersonPlan("t214", 214L, null, null, 0, Activity.LIGHT, "송천1동",
                    List.of(new PetPlan("bami", "밤이", true, "포메라니안", null, true, 3 * 12, 2.8, 2.8, null, true, "갈색",
                            "초인종 소리에 제일 먼저 짖어요.", null, null, 1))),
            new PersonPlan("t215", 215L, null, null, 0, Activity.LIGHT, "덕진동",
                    List.of(new PetPlan("yulmu", "율무", true, "말티푸", null, false, 8, 2.9, 2.9, null, false, "크림",
                            "오늘 가입했어요. 아직 산책 연습 중인 아기 강아지예요.", null, null, 0))),
            new PersonPlan("t216", 216L, null, null, 0, Activity.LIGHT, "진북동",
                    List.of(new PetPlan("bom", "봄이", false, "먼치킨", null, false, 7, 2.8, 2.8, null, false, "치즈",
                            "오늘 가입했어요. 다리는 짧아도 점프는 잘해요.", null, null, 0))),
            // 탈퇴한 테스트 계정 : 탈퇴 전에 반려동물만 등록했다(활동 기록은 만들지 않는다)
            new PersonPlan("t203", 203L, null, null, 0, Activity.NONE, "금암동",
                    List.of(new PetPlan("kkami", "까미", true, "푸들", null, true, 5 * 12, 4.2, 4.2, null, true, "검정", null, null, null, 0))),
            new PersonPlan("t204", 204L, null, null, 0, Activity.NONE, "서신동",
                    List.of(new PetPlan("ari", "아리", false, "샴", null, false, 2 * 12, 3.4, 3.4, null, true, "포인트", null, null, null, 0))),
            new PersonPlan("t208", 208L, null, null, 0, Activity.NONE, "인후1동",
                    List.of(new PetPlan("dori", "도리", true, "비글", null, true, 4 * 12, 11.2, 11.2, null, true, "삼색", null, null, null, 0))),
            new PersonPlan("t210", 210L, null, null, 0, Activity.NONE, "덕진동",
                    List.of(new PetPlan("hodu", "호두", true, "슈나우저", null, true, 6 * 12, 7.4, 7.4, null, true, "회색", null, null, null, 0))),
            new PersonPlan("t211", 211L, null, null, 0, Activity.NONE, "송천1동",
                    List.of(new PetPlan("mimi", "미미", false, "터키시 앙고라", null, false, 3 * 12, 3.7, 3.7, null, true, "흰색", null, null, null, 0))));

    /** 새 회원 계획 전부(기존 엑스트라 + 새 엑스트라 55명) */
    static List<PersonPlan> everyExtra() {
        List<PersonPlan> all = new java.util.ArrayList<>(EXTRAS);
        all.addAll(ExtraPeople.PLANS);
        return all;
    }

    private People() {
    }
}
