package com.nyangmeong.seed.domain;

import java.util.List;
import java.util.Map;

import com.nyangmeong.seed.core.World.Activity;

/**
 * [새 엑스트라 55명]
 * - 활동량 : 자주 6·보통 15·가끔 17·눈팅 10·휴면 7. 닉네임·메일·지역·반려동물·가입일이 서로 맞는 사람처럼 정한다
 * - 가입일은 최근 1년이고 최근 90일에 더 몰려 있다. 메일은 MemberSeeder.EMAIL_DOMAIN
 * - 휴면·정지·탈퇴는 META에 적는다(활동이 끝난 날 : 이 회원의 글·댓글·기록은 그날 앞에만 있다)
 * - 글·댓글은 ExtraPosts, 신고는 ReportSeeder
 */
final class ExtraPeople {

    enum Status {
        NORMAL, SUSPENDED, BANNED, WITHDRAWN
    }

    /**
     * @param daysAgo 휴면 시작·정지·탈퇴가 며칠 전인지
     * @param reason  정지 사유(상태 이력 변경 사유)
     */
    record Meta(Status status, int daysAgo, String reason) {
    }

    static final Map<String, Meta> META = Map.ofEntries(
            Map.entry("rude", new Meta(Status.SUSPENDED, 2, "신고 누적 처리: 욕설·비방 댓글")),
            Map.entry("wdMong", new Meta(Status.WITHDRAWN, 24, null)),
            Map.entry("wdSubak", new Meta(Status.WITHDRAWN, 17, null)),
            Map.entry("wdDodam", new Meta(Status.WITHDRAWN, 11, null)),
            Map.entry("wdYeondu", new Meta(Status.WITHDRAWN, 5, null)),
            Map.entry("spamAd", new Meta(Status.BANNED, 5, "스팸·광고성 게시물 반복(영구 정지)")),
            Map.entry("minimom", new Meta(Status.NORMAL, 112, null)),
            Map.entry("bokshil", new Meta(Status.NORMAL, 95, null)),
            Map.entry("sundol", new Meta(Status.NORMAL, 85, null)),
            Map.entry("chichi", new Meta(Status.NORMAL, 78, null)),
            Map.entry("kkomi", new Meta(Status.NORMAL, 70, null)),
            Map.entry("sloth", new Meta(Status.NORMAL, 66, null)),
            Map.entry("hyung", new Meta(Status.NORMAL, 62, null)));

    static final List<People.PersonPlan> PLANS = List.of(
            new People.PersonPlan("madeleine", null, "마들렌엄마", "madeleine", 310, Activity.HEAVY, "효자2동",
                    List.of(new People.PetPlan("madeleine", "마들렌", true, "푸들", null, false, 36, 4.4, 4.3, null, true, "갈색", "산책 나가면 만나는 강아지마다 인사부터 해요.", null, null, 300))),
            new People.PersonPlan("jjanggu", null, "짱구아빠", "jjangguba", 280, Activity.HEAVY, "삼천1동",
                    List.of(new People.PetPlan("jjanggu", "짱구", true, "비글", null, true, 48, 11.8, 11.4, null, true, "삼색", "코로 바닥을 훑느라 산책이 느려요.", null, null, 270),
                            new People.PetPlan("jjanga", "짱아", false, "코리안 숏헤어", null, false, 30, 3.9, 4.0, null, true, "치즈", "짱구 꼬리를 잡는 게 취미예요.", null, null, 200))),
            new People.PersonPlan("mintcho", null, "민트초코집사", "mintchoco", 240, Activity.HEAVY, "평화1동",
                    List.of(new People.PetPlan("mint", "민트", false, "러시안 블루", null, true, 24, 4.5, 4.6, null, true, "회색", "창가에서 새 구경만 해요.", null, null, 230),
                            new People.PetPlan("choco2", "초코", false, "브리티시 숏헤어", null, false, 24, 3.8, 3.8, null, true, "회색", "민트 옆에 꼭 붙어 자요.", null, null, 230))),
            new People.PersonPlan("haru", null, "하루산책", "haruwalk", 190, Activity.HEAVY, "혁신동",
                    List.of(new People.PetPlan("haru", "하루", true, "골든 리트리버", null, true, 60, 31.0, 30.2, null, true, "황색", "공이라면 물불을 안 가려요.", null, null, 180))),
            new People.PersonPlan("soso", null, "소소한오후", "sosohan", 150, Activity.HEAVY, "중화산1동",
                    List.of(new People.PetPlan("bosong", "보송", true, "말티푸", null, false, 30, 4.1, 4.2, null, true, "크림", "낯선 사람도 처음부터 반가워해요.", null, null, 140))),
            new People.PersonPlan("godeung", null, "고등어네", "godeungeo", 120, Activity.HEAVY, "우아2동",
                    List.of(new People.PetPlan("godeung", "고등", false, "코리안 숏헤어", null, true, 36, 5.2, 5.0, null, true, "고등어", "식탐이 대단해요.", null, null, 110),
                            new People.PetPlan("mu", "무", false, "코리안 숏헤어", null, true, 36, 4.9, 4.9, null, true, "고등어", "고등이 뒤만 졸졸 따라다녀요.", null, null, 110))),
            new People.PersonPlan("mungchi", null, "뭉치누나", "mungchi", 340, Activity.NORMAL, "서신동",
                    List.of(new People.PetPlan("mungchi", "뭉치", true, "시츄", null, true, 72, 7.6, 7.5, null, true, "갈색", "나이가 들수록 느긋해졌어요.", null, null, 330))),
            new People.PersonPlan("dallae", null, "달래네", "dallae", 260, Activity.NORMAL, "효자1동",
                    List.of(new People.PetPlan("dallae", "달래", true, "시바견", null, false, 30, 8.1, 8.0, null, true, "황색", "고집은 세지만 애교도 많아요.", null, null, 250))),
            new People.PersonPlan("byeoli", null, "별이아빠", "byeoliba", 220, Activity.NORMAL, "송천2동",
                    List.of(new People.PetPlan("byeoli", "별이", false, "아비시니안", null, true, 26, 4.1, 4.2, null, true, "갈색", "캣타워 꼭대기가 자기 자리예요.", null, null, 210))),
            new People.PersonPlan("gamja", null, "감자와고구마", "gamjagogu", 200, Activity.NORMAL, "호성동",
                    List.of(new People.PetPlan("gamja", "감자", true, "닥스훈트", null, false, 60, 6.5, 6.4, null, true, "갈색", "허리 때문에 계단은 안아서 올려요.", null, null, 190),
                            new People.PetPlan("gogu", "고구마", true, "닥스훈트", null, true, 36, 7.0, 7.2, null, true, "갈색", "먹을 걸 보면 점프를 해요.", null, null, 150))),
            new People.PersonPlan("omwol", null, "오월의집사", "omwol", 180, Activity.NORMAL, "인후3동",
                    List.of(new People.PetPlan("omwol", "오월", false, "샴", null, false, 40, 3.7, 3.6, null, true, "포인트", "말이 많은 고양이예요.", null, null, 170))),
            new People.PersonPlan("tori", null, "토리토리", "toritori", 170, Activity.NORMAL, "금암동",
                    List.of(new People.PetPlan("tori", "토리", true, "포메라니안", null, true, 48, 2.9, 2.9, null, true, "크림", "털 빠짐이 장난 아니에요.", null, null, 160))),
            new People.PersonPlan("sarang", null, "사랑이누나", "sarangi", 150, Activity.NORMAL, "조촌동",
                    List.of(new People.PetPlan("sarang", "사랑", true, "진돗개", null, false, 84, 16.0, 15.6, null, true, "황색", "아파트에서도 얌전한 진돗개예요.", null, null, 140))),
            new People.PersonPlan("punch", null, "냥냥펀치", "nyangpunch", 140, Activity.NORMAL, "서신동",
                    List.of(new People.PetPlan("punch", "펀치", false, "먼치킨", null, true, 22, 3.9, 4.1, null, true, "치즈", "짧은 다리로 점프를 시도해요.", null, null, 130))),
            new People.PersonPlan("kongtteok", null, "콩떡네", "kongtteok", 125, Activity.NORMAL, "효자3동",
                    List.of(new People.PetPlan("kongtteok", "콩떡", true, "치와와", null, false, 70, 2.8, 2.7, null, true, "갈색", "추위를 많이 타서 겨울엔 옷이 필수예요.", null, null, 120))),
            new People.PersonPlan("summer", null, "여름밤산책", "summernight", 110, Activity.NORMAL, "평화2동",
                    List.of(new People.PetPlan("summer", "여름", true, "보더 콜리", null, false, 36, 17.2, 17.0, null, true, "검정", "에너지가 넘쳐서 하루 두 시간은 걸어요.", null, null, 100))),
            new People.PersonPlan("hairball", null, "털뭉치제작소", "hairballs", 100, Activity.NORMAL, "진북동",
                    List.of(new People.PetPlan("hairball", "털뭉", false, "메인쿤", null, true, 30, 6.4, 6.6, null, true, "갈색", "덩치는 큰데 겁이 많아요.", null, null, 90))),
            new People.PersonPlan("sunflower", null, "해바라기집", "haebaragi", 90, Activity.NORMAL, "우아1동",
                    List.of(new People.PetPlan("haebak", "해바", true, "요크셔 테리어", null, true, 96, 3.3, 3.2, null, true, "블랙탄", "나이에 비해 정정해요.", null, null, 85))),
            new People.PersonPlan("pudding", null, "푸딩이", "pudding", 80, Activity.NORMAL, "삼천3동",
                    List.of(new People.PetPlan("pudding", "푸딩", true, "프렌치 불독", null, true, 28, 12.5, 12.8, null, true, "크림", "코골이가 심해서 같이 자면 웃겨요.", null, null, 75))),
            new People.PersonPlan("latte", null, "라떼는말이야", "lattemal", 70, Activity.NORMAL, "덕진동",
                    List.of(new People.PetPlan("latte", "라떼", false, "스코티시 폴드", null, true, 24, 4.3, 4.3, null, true, "크림", "사람 말을 알아듣는 것 같아요.", null, null, 65))),
            new People.PersonPlan("rude", null, "팩트폭행기", "factbang", 150, Activity.NORMAL, "진북동",
                    List.of()),
            new People.PersonPlan("wdMong", null, "하늘구름", "haneulgureum", 300, Activity.LIGHT, "풍남동",
                    List.of(new People.PetPlan("wdMong", "몽이", true, "말티즈", null, false, 60, 3.4, 3.4, null, true, "흰색", "소심한 말티즈예요.", null, null, 290))),
            new People.PersonPlan("wdSubak", null, "수박바", "subakba", 260, Activity.LIGHT, "완산동",
                    List.of(new People.PetPlan("wdSubak", "수박", false, "코리안 숏헤어", null, false, 18, 3.6, 3.8, null, true, "삼색", "삼색이라 털이 알록달록해요.", null, null, 250))),
            new People.PersonPlan("wdDodam", null, "평일저녁", "pyeongil", 230, Activity.LIGHT, "중앙동",
                    List.of(new People.PetPlan("wdDodam", "도담", true, "시츄", null, false, 84, 6.9, 6.8, null, true, "흰색", "조용한 노견이에요.", null, null, 220))),
            new People.PersonPlan("wdYeondu", null, "연두", "yeondu", 205, Activity.LIGHT, "동서학동",
                    List.of(new People.PetPlan("wdYeondu", "연두", false, "노르웨이 숲", null, false, 20, 4.1, 4.3, null, true, "크림", "발소리가 거의 안 나요.", null, null, 195))),
            new People.PersonPlan("rookie", null, "초보집사", "chobo", 160, Activity.LIGHT, "노송동",
                    List.of(new People.PetPlan("rookie", "다롱", false, "페르시안", null, true, 14, 3.6, 3.9, null, false, "흰색", "눈이 커서 항상 놀란 표정이에요.", null, null, 150))),
            new People.PersonPlan("rainy", null, "비오는날", "rainyday", 150, Activity.LIGHT, "효자5동",
                    List.of(new People.PetPlan("rainy", "비오", true, "푸들", null, true, 60, 4.0, 4.0, null, true, "검정", "비 오는 날 산책을 제일 싫어해요.", null, null, 140))),
            new People.PersonPlan("dongdong", null, "동동이", "dongdong", 135, Activity.LIGHT, "팔복동",
                    List.of(new People.PetPlan("dongdong", "동동", true, "진돗개", null, true, 24, 18.5, 19.1, null, false, "황색", "아직 철이 없어서 매일 신나요.", null, null, 130))),
            new People.PersonPlan("jamong", null, "자몽에이드", "jamongade", 120, Activity.LIGHT, "삼천2동",
                    List.of(new People.PetPlan("jamong", "자몽", false, "터키시 앙고라", null, false, 30, 3.3, 3.3, null, true, "흰색", "눈 색이 서로 달라요.", null, null, 110))),
            new People.PersonPlan("bbang", null, "빵빵이네", "bbangbbang", 95, Activity.LIGHT, "여의동",
                    List.of(new People.PetPlan("bbang", "빵빵", true, "퍼그", null, true, 48, 9.4, 9.6, null, true, "황색", "먹을 때만 엄청 빨라요.", null, null, 90))),
            new People.PersonPlan("dotori", null, "도토리집", "dotorijip", 60, Activity.LIGHT, "서서학동",
                    List.of(new People.PetPlan("dotori", "도토리", true, "말티푸", null, true, 16, 3.8, 4.1, null, false, "갈색", "이제 막 산책 연습 중이에요.", null, null, 55))),
            new People.PersonPlan("maru", null, "마루네", "maruine", 50, Activity.LIGHT, "중화산2동",
                    List.of(new People.PetPlan("maru", "마루", true, "슈나우저", null, true, 60, 7.6, 7.5, null, true, "회색", "수염 관리가 제일 일이에요.", null, null, 45))),
            new People.PersonPlan("platy", null, "오리너구리", "orineoguri", 40, Activity.LIGHT, "호성동",
                    List.of(new People.PetPlan("platy", "오리", false, "벵갈", null, true, 28, 4.7, 4.7, null, true, "갈색", "활동량이 어마어마해요.", null, null, 35))),
            new People.PersonPlan("dawn", null, "새벽두시", "saebyeok2", 30, Activity.LIGHT, "인후2동",
                    List.of(new People.PetPlan("dawn", "가을", false, "코리안 숏헤어", null, false, 36, 3.9, 3.8, null, true, "삼색", "밤에 제일 활발해요.", null, null, 25))),
            new People.PersonPlan("yunseul", null, "윤슬", "yunseul", 20, Activity.LIGHT, "송천3동",
                    List.of(new People.PetPlan("yunseul", "윤슬", true, "시바견", null, false, 10, 6.1, 7.0, null, false, "황색", "성장기라 하루가 다르게 커요.", null, null, 15))),
            new People.PersonPlan("mocha", null, "모카라떼", "mochalatte", 12, Activity.LIGHT, "혁신동",
                    List.of(new People.PetPlan("mocha", "모카", true, "푸들", null, false, 14, 3.7, 3.9, null, false, "갈색", "입양한 지 얼마 안 됐어요.", null, null, 10))),
            new People.PersonPlan("spamAd", null, "펫용품싸게", "petcheap", 38, Activity.LIGHT, "덕진동",
                    List.of()),
            new People.PersonPlan("gonggu", null, "공구총대", "gonggu", 85, Activity.LIGHT, "금암동",
                    List.of(new People.PetPlan("gonggu", "두리", true, "말티즈", null, true, 40, 3.6, 3.6, null, true, "흰색", "공구하는 집사 옆을 지키는 강아지예요.", null, null, 80))),
            new People.PersonPlan("yunha", null, "윤하네", "yunhane", 200, Activity.LURKER, "서신동",
                    List.of(new People.PetPlan("yunha", "하니", true, "푸들", null, false, 50, 4.2, 4.2, null, true, "크림", "가끔 짖는 것 빼면 얌전해요.", null, null, 190))),
            new People.PersonPlan("seasons", null, "사계절", "sagyejeol", 170, Activity.LURKER, "효자4동",
                    List.of(new People.PetPlan("seasons", "가을이", false, "코리안 숏헤어", null, false, 44, 4.0, 4.1, null, true, "고등어", "계절마다 털갈이를 해요.", null, null, 160))),
            new People.PersonPlan("springcat", null, "봄날의고양이", "bomnal", 140, Activity.LURKER, "중앙동",
                    List.of(new People.PetPlan("springcat", "봄", false, "랙돌", null, false, 28, 4.8, 5.0, null, true, "포인트", "안으면 늘어지는 랙돌이에요.", null, null, 130))),
            new People.PersonPlan("bora", null, "보라돌이", "boradori", 110, Activity.LURKER, "인후1동",
                    List.of(new People.PetPlan("bora", "보라", true, "치와와", null, false, 84, 2.5, 2.5, null, true, "크림", "나이가 많아 조용해요.", null, null, 100))),
            new People.PersonPlan("smallroom", null, "작은방", "jakeunbang", 85, Activity.LURKER, "진북동",
                    List.of()),
            new People.PersonPlan("doran", null, "도란도란", "doran", 65, Activity.LURKER, "삼천1동",
                    List.of(new People.PetPlan("doran", "도란", true, "스피츠", null, true, 36, 6.3, 6.4, null, true, "흰색", "털이 풍성해서 눈사람 같아요.", null, null, 60))),
            new People.PersonPlan("dandelion", null, "민들레", "mindeulle", 45, Activity.LURKER, "평화1동",
                    List.of(new People.PetPlan("dandelion", "들레", false, "코리안 숏헤어", null, false, 12, 3.0, 3.5, null, false, "치즈", "길에서 데려온 지 한 달 됐어요.", null, null, 40))),
            new People.PersonPlan("tangerine", null, "귤밭", "gyulbat", 25, Activity.LURKER, "송천1동",
                    List.of(new People.PetPlan("tangerine", "귤", false, "먼치킨", null, true, 10, 2.9, 3.4, null, false, "치즈", "아직 아기라 매일 사고를 쳐요.", null, null, 20))),
            new People.PersonPlan("seaside", null, "바다소리", "badasori", 15, Activity.LURKER, "효자2동",
                    List.of()),
            new People.PersonPlan("kongnamul", null, "콩나물국", "kongnamulguk", 8, Activity.LURKER, "덕진동",
                    List.of(new People.PetPlan("kongnamul", "콩나물", true, "말티즈", null, true, 18, 3.1, 3.2, null, false, "흰색", "입양 첫 주라 모든 게 신기해요.", null, null, 6))),
            new People.PersonPlan("minimom", null, "미미엄마", "mimimom", 330, Activity.DORMANT, "서신동",
                    List.of(new People.PetPlan("mimi2", "미미", true, "시츄", null, false, 96, 6.5, 6.5, null, true, "갈색", "나이 든 시츄예요.", null, null, 320))),
            new People.PersonPlan("bokshil", null, "복실이네", "bokshil", 280, Activity.DORMANT, "효자3동",
                    List.of(new People.PetPlan("bokshil", "복실", true, "골든 리트리버", null, false, 72, 27.0, 27.3, null, true, "황색", "털이 복실복실해요.", null, null, 270))),
            new People.PersonPlan("sundol", null, "순돌아빠", "sundolba", 250, Activity.DORMANT, "우아1동",
                    List.of(new People.PetPlan("sundol", "순돌", true, "진돗개", null, true, 48, 18.0, 18.2, null, true, "황색", "집을 잘 지켜요.", null, null, 240))),
            new People.PersonPlan("chichi", null, "치치집사", "chichi", 230, Activity.DORMANT, "금암동",
                    List.of(new People.PetPlan("chichi", "치치", false, "코리안 숏헤어", null, true, 60, 4.8, 4.9, null, true, "치즈", "자기 전에 꼭 이불 속으로 파고들어요.", null, null, 220))),
            new People.PersonPlan("kkomi", null, "꼬미누나", "kkomi", 210, Activity.DORMANT, "평화2동",
                    List.of(new People.PetPlan("kkomi", "꼬미", true, "요크셔 테리어", null, true, 40, 3.0, 3.0, null, true, "블랙탄", "작아도 겁이 없어요.", null, null, 200))),
            new People.PersonPlan("sloth", null, "나무늘보", "namuneulbo", 200, Activity.DORMANT, "조촌동",
                    List.of(new People.PetPlan("sloth", "느리", false, "러시안 블루", null, false, 36, 3.9, 4.0, null, true, "회색", "동작이 느려서 별명이 느리예요.", null, null, 190))),
            new People.PersonPlan("hyung", null, "동네형", "dongnehyung", 190, Activity.DORMANT, "호성동",
                    List.of(new People.PetPlan("hyung", "대장", true, "보더 콜리", null, true, 48, 18.5, 18.3, null, true, "검정", "동네 개들의 대장 노릇을 해요.", null, null, 180))));

    private ExtraPeople() {
    }
}
