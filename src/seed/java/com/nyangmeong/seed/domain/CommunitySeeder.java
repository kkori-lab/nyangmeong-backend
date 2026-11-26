package com.nyangmeong.seed.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.seed.core.Codes;
import com.nyangmeong.seed.core.Masters;
import com.nyangmeong.seed.core.Row;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.SeedWarnings;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.World;

/**
 * [커뮤니티]
 * - 사람이 쓴 것처럼 글·댓글을 하나씩 정해 둔다(무작위 조합 문장 없음). 사진은 넣지 않는다
 * - 글쓴이·댓글 단 사람은 People의 key. 그 회원이 없거나 가입 전이면 그 글·댓글은 건너뛴다
 * - 실종센터 : 주인공이 덕진공원에서 콩이를 잃어버렸다 찾은 글(찾았어요), 수줍은집사의 나비(실종 중)
 * - 우리동네 펫돌봄 : 주인공의 보리 산책 모집(단추아빠와 채팅 중), 치즈집사의 방문 돌봄 모집
 * - 주인공 글에 달린 댓글은 주인공 알림(World.alarms)으로 넘긴다
 */
public class CommunitySeeder implements Seeder {

    private static final String POST_PATH = "/community/posts/%d";
    private static final String LOST_PATH = "/community/missing/%d";
    private static final String CHAT_PATH = "/chat?otoChatRoomSn=%d";

    /** 댓글 : key가 단 글, reply는 앞 댓글 번호(0부터, 없으면 -1), minutes는 글 올린 뒤 몇 분 */
    private record C(String key, String text, int reply, int minutes) {
    }

    private record P(String key, String bbs, int daysAgo, int hour, String title, String content, int likes, List<C> comments) {
    }

    private static C c(String key, String text, int minutes) {
        return new C(key, text, -1, minutes);
    }

    private static C r(String key, String text, int reply, int minutes) {
        return new C(key, text, reply, minutes);
    }

    private static final List<P> POSTS = List.of(
            new P("persona", "펫일상", 84, 22, "콩이 슬개골 2기 진단 받았어요",
                    "산책하다가 콩이가 왼쪽 뒷다리를 들고 깽깽이처럼 걷길래 바로 병원 다녀왔어요.\n\n슬개골 탈구 2기래요. 11살이라 수술보다는 체중 유지하고 관절 영양제, 짧은 산책으로 관리해 보자고 하셨어요.\n\n집 바닥이 미끄러워서 매트부터 주문했습니다. 노견 키우시는 분들 매트 어떤 거 쓰세요?",
                    9, List.of(c("inhuPom", "저희 솜이도 슬개골 1기예요ㅠㅠ 퍼즐매트보다 롤매트가 틈이 없어서 좋더라고요.", 38),
                            r("persona", "롤매트 찾아볼게요! 감사해요 🙏", 0, 61), c("boriDad", "소파 올라가는 계단도 하나 사자", 95),
                            c("songcheon", "콩이 빨리 괜찮아지길 바라요. 저희도 계단 두고 나서 훨씬 덜 뛰어요.", 410))),
            new P("persona", "펫정보", 60, 21, "노령견 슬개골 관리 3주 해 보고 달라진 점",
                    "진단받고 3주 동안 해 본 것 정리해요.\n\n1. 거실·복도에 롤매트 깔기\n2. 소파·침대에 계단 두기\n3. 산책은 15~20분씩 하루 두 번으로 나누기\n4. 관절 영양제(글루코사민·초록입홍합) 매일\n5. 발바닥 털 자주 정리하기\n\n요즘은 다리 드는 횟수가 확실히 줄었어요. 체중도 3.5kg에서 3.4kg으로 조금 빠졌고요. 병원에서도 지금처럼만 하라고 하셨어요.",
                    14, List.of(c("inhuPom", "정리 감사해요! 발바닥 털 정리는 생각 못 했네요.", 120), c("mallang", "저장해 둡니다 👍", 300),
                            c("t207", "저희 초코도 디스크 때문에 계단 쓰는데 정말 도움 돼요.", 2000))),
            new P("persona", "펫일상", 33, 23, "보리 다이어트 한 달째 기록",
                    "정기검진에서 과체중 판정받고 다이어트 시작한 지 한 달 됐어요.\n\n14.2kg → 13.8kg\n\n사료를 10% 줄이고 다이어트 사료로 바꿨고, 간식은 하루 3개까지만 줘요. 산책은 아침·저녁 두 번은 꼭 가고요.\n\n밥그릇 비우고 저를 쳐다보는 눈빛이 너무 슬프지만... 목표는 12.5kg입니다!",
                    11, List.of(c("boriDad", "저녁 산책은 내가 더 길게 돌게", 45), c("cheese", "코기 엉덩이 사수하세요 ㅋㅋㅋ 응원합니다!", 130),
                            r("persona", "엉덩이는 지킬게요 ㅋㅋ", 1, 180), c("songcheon", "주말에 건지산 같이 걸어요! 단추도 살 좀 빼야 해요.", 520))),
            new P("persona", "펫정보", 25, 22, "고양이 신장병 진단 후 처방식 바꿀 때 팁",
                    "두부가 9월에 만성 신장병 2기 진단을 받았어요. 처방식으로 바꾸면서 겪은 것들 공유해요.\n\n- 한 번에 바꾸지 말고 기존 사료에 조금씩 섞어서 1~2주 동안 늘리기\n- 건식이 안 맞으면 같은 처방식의 습식 파우치로 먼저 적응시키기\n- 물그릇을 집 곳곳에 두고, 분수대도 하나 놔 주기\n- 매일 음수량을 기록해서 병원 갈 때 보여 드리기\n\n지난달 재검사에서 수치가 조금 좋아졌어요. 꾸준히 하는 게 제일인 것 같아요.",
                    17, List.of(c("cheese", "음수량 기록 정말 중요하죠. 저도 분수대 들이고 나서 치즈가 물을 훨씬 많이 마셔요.", 70),
                            c("shy", "나비도 나이 들면 걱정이라 미리 저장해요.", 400), c("seosin", "좋은 정보 감사합니다!", 900))),
            new P("persona", "펫일상", 3, 21, "보리 13.3kg 찍었어요!!",
                    "오늘 아침에 쟀더니 13.3kg이에요 🎉\n시작할 때 14.2kg이었으니까 0.9kg 뺐어요.\n\n다음 주에 병원 가서 재 보기로 했는데 칭찬받았으면 좋겠네요.",
                    15, List.of(c("boriDad", "보리 대견하다", 12), c("songcheon", "와 축하해요!! 단추는 그대로인데 ㅋㅋㅋ", 85),
                            c("inhuPom", "꾸준함의 승리네요 👏", 240), c("t209", "저희 누렁이도 다이어트 해야 하는데 자극받고 가요", 1300))),
            new P("cheese", "펫정보", 150, 20, "고양이 모래 5종 써 본 후기",
                    "두 냥이 키우면서 1년 동안 써 본 모래 정리해 봤어요.\n\n벤토나이트 : 응고력 최고, 먼지는 좀 있어요\n두부모래 : 변기에 버릴 수 있어서 편한데 망고가 싫어해요\n카사바 : 가볍고 먼지 적어요. 대신 가격이 있어요\n크리스탈 : 냄새는 잘 잡는데 발에 밟히는 느낌을 싫어하는 애들이 있어요\n펠렛 : 사막화 없는데 적응 기간이 필요해요\n\n저희는 결국 벤토나이트로 정착했어요.",
                    13, List.of(c("shy", "저희도 결국 벤토나이트로 돌아왔어요 ㅎㅎ", 300), c("songcheon", "강아지만 키워서 몰랐는데 종류가 이렇게 많군요", 900))),
            new P("cheese", "펫일상", 95, 22, "망고 털갈이 시즌 시작",
                    "빗질 한 번 하면 고양이 한 마리가 더 나와요...\n노르웨이 숲 키우시는 분들 털 어떻게 관리하세요? 청소기를 하루 두 번 돌려도 끝이 없네요.",
                    8, List.of(c("seosin", "랙돌도 만만치 않아요 ㅠㅠ 고무장갑 빗질 추천해요!", 200), r("cheese", "고무장갑 써 볼게요!", 0, 260))),
            new P("cheese", "펫정보", 40, 21, "고양이 물 많이 먹이는 방법",
                    "고양이는 원래 물을 잘 안 마셔서 신장·방광 질환에 약하대요. 저희 집에서 효과 있었던 것들이에요.\n\n1. 세라믹 분수대\n2. 밥그릇과 물그릇 멀리 떨어뜨리기\n3. 습식 사료에 물 한 숟가락 섞기\n4. 넓고 얕은 그릇 쓰기(수염이 안 닿게)\n\n치즈는 분수대 들이고 나서 하루 음수량이 150ml에서 230ml로 늘었어요.",
                    16, List.of(c("persona", "두부도 분수대 놓고 나서 물을 훨씬 잘 마셔요. 좋은 글이에요!", 90), c("t202", "얕은 그릇 바로 바꿔 봐야겠어요", 3000))),
            new P("cheese", "펫일상", 6, 22, "치즈 다이어트 시작합니다",
                    "건강검진에서 5.9kg 나와서 수의사 선생님께 혼났어요 😂\n목표는 5.2kg! 하루 급여량 재서 주고 낚싯대 놀이 15분씩 하기로 했어요.\n@콩이네 님 보리 다이어트 글 보고 용기 냈어요.",
                    7, List.of(c("persona", "같이 힘내요!! 기록하면 확실히 도움 돼요", 40), c("t213", "치즈 이름처럼 동글동글 귀여워요 ㅎㅎ", 600))),
            new P("songcheon", "펫일상", 120, 19, "건지산 산책로 추천해요",
                    "전주 덕진 쪽 사시는 분들 건지산 편백숲 길 추천해요.\n\n오송제 쪽에서 올라가면 경사가 완만해서 노견도 걸을 만하고, 그늘이 많아서 여름에도 괜찮아요. 주말 오전에는 강아지 친구들도 많이 만나요.\n\n배변봉투는 꼭 챙겨 주세요!",
                    18, List.of(c("persona", "저도 보리랑 자주 가요! 오송제 쪽 좋죠", 150), c("t205", "해피 데리고 가 봐야겠네요", 2500),
                            c("mallang", "떡이랑 주말에 가 볼게요", 4000))),
            new P("songcheon", "펫정보", 70, 21, "시바견 털갈이 빗 추천",
                    "단추 키운 지 5년 동안 써 본 빗 중에 제일 좋았던 건 슬리커 브러시 + 언더코트 레이크 조합이에요.\n\n털갈이 시즌에는 레이크로 속털을 먼저 빼고 슬리커로 정리하면 시간이 반으로 줄어요. 너무 세게 빗으면 피부가 상하니까 살살!",
                    9, List.of(c("cheese", "고양이한테도 레이크 써도 될까요?", 300), r("songcheon", "장모 고양이는 괜찮다고 들었어요. 살살만 하시면 돼요!", 0, 420))),
            new P("songcheon", "펫일상", 15, 18, "단추가 또 산책 중에 드러누웠어요 ㅋㅋ",
                    "집에 가기 싫다고 길바닥에 드러누워서 10분 동안 협상했어요.\n시바 키우시는 분들은 다 아시죠... 결국 간식 하나 주고 데려왔습니다.",
                    21, List.of(c("persona", "ㅋㅋㅋㅋ 시바 거부 너무 귀여워요", 30), c("boriDad", "보리도 가끔 저래요 ㅋㅋ", 200),
                            c("t214", "상상만 해도 귀엽네요 ㅋㅋ", 4000))),
            new P("songcheon", "펫일상", 1, 8, "오늘 아침 송천동 안개 산책",
                    "안개가 자욱해서 단추랑 천천히 걸었어요. 오늘 아침 엄청 쌀쌀하네요. 다들 산책하실 때 옷 따뜻하게 입히세요!",
                    5, List.of(c("persona", "저도 아침에 보리랑 나갔다가 추워서 금방 들어왔어요 ㅎㅎ", 90))),
            new P("inhuPom", "펫정보", 50, 22, "노령 포메 기관지 협착, 겨울 대비 이렇게 해요",
                    "솜이가 기관지 협착이 있어서 겨울이 제일 걱정이에요. 수의사 선생님이 알려 주신 것들 공유해요.\n\n- 목줄 말고 하네스 쓰기\n- 가습기로 습도 50~60% 맞추기\n- 흥분하면 기침이 심해지니 산책 전에 차분하게\n- 체중 늘지 않게 관리\n\n밤에 거위 소리처럼 기침하면 꼭 병원 가 보세요.",
                    12, List.of(c("persona", "콩이도 노견이라 하네스로 바꿨어요. 습도는 생각 못 했네요!", 60), c("t214", "포메 키우는데 미리 알아 둘게요", 3500))),
            new P("inhuPom", "펫일상", 9, 20, "솜이 10살 생일이에요",
                    "오늘 솜이 10살 생일이라 강아지 케이크 사 왔어요 🎂\n요즘 잠이 많아졌지만 아직 산책 나가면 꼬리 흔들면서 잘 걸어요. 오래오래 같이 살자 솜아.",
                    19, List.of(c("persona", "솜이 생일 축하해요!! 🎉", 25), c("songcheon", "축하해요 솜이!", 140), c("shy", "생일 축하해 솜아 🎂", 600))),
            new P("shy", "펫일상", 45, 23, "나비가 처음으로 무릎에 올라왔어요",
                    "데려온 지 1년 만에 처음으로 무릎에 올라와서 잠들었어요.\n다리가 저린데 움직일 수가 없네요... 행복합니다.",
                    16, List.of(c("cheese", "그 순간 못 움직이는 거 너무 공감돼요 ㅋㅋ", 50), c("seosin", "부러워요!! 구름이는 아직 안 올라와요", 300))),
            new P("mallang", "펫정보", 22, 21, "비숑 미용 주기 어떻게 하세요?",
                    "떡이가 털이 너무 빨리 자라서 고민이에요. 지금은 6주마다 전체 미용하는데 중간에 얼굴만 다듬는 게 나을까요? 비숑 키우시는 분들 의견 궁금해요.",
                    4, List.of(c("persona", "저희 콩이(말티즈)는 한 달에 한 번 위생미용만 하고 전체는 두 달에 한 번 해요.", 70),
                            r("mallang", "위생미용만 따로 하는 방법도 있군요. 감사해요!", 0, 150), c("t201", "저희도 얼굴만 중간에 다듬어요", 1500))),
            new P("seosin", "펫일상", 30, 20, "구름이 첫 예방접종 다녀왔어요",
                    "병원에서 너무 얌전해서 칭찬받았어요. 집에 오자마자 기절하듯 잠들었네요 😴",
                    10, List.of(c("cheese", "아기 고양이 첫 접종이라니 귀엽다 ㅎㅎ", 80), c("persona", "구름이 수고했어요!", 200))),
            new P("t207", "펫일상", 5, 21, "디스크 수술 2년, 이제 잘 뛰어요",
                    "초코가 2024년에 디스크 수술했는데 지금은 공원에서 잘 뛰어다녀요. 수술 직후 사진 보면 아직도 마음이 아파요.\n수술 고민하시는 분들 계시면 경험 나눌게요.",
                    13, List.of(c("persona", "정말 다행이에요. 콩이도 관절 때문에 걱정이 많아서 힘이 되네요.", 50), c("inhuPom", "초코 대단해요!", 400))),
            new P("t209", "펫일상", 2, 20, "누렁이 우리 집에 온 지 5년",
                    "보호소에서 처음 만났을 때 구석에서 떨던 아이가 이제는 소파를 다 차지하고 자요 ㅎㅎ\n유기견 입양 고민하시는 분들, 정말 추천해요.",
                    20, List.of(c("persona", "누렁이 너무 행복해 보여요 💛", 40), c("songcheon", "멋진 가족이에요!", 300), c("shy", "감동이에요 ㅠㅠ", 700))),
            new P("t201", "펫일상", 4, 22, "쿠키 공놀이 영상 찍으려다 실패",
                    "공을 던지면 가져오긴 하는데 절대 안 놔줘요 ㅋㅋㅋ 푸들 원래 이런가요?", 6,
                    List.of(c("mallang", "떡이도 그래요 ㅋㅋ 바꿔 줄 간식이 필요해요", 120))),
            new P("t206", "펫정보", 7, 22, "스코티시 폴드 관절 관리 질문이요",
                    "모모가 폴드라 관절이 약하다고 해서 캣타워를 낮은 걸로 바꿨어요. 다른 집사님들은 어떻게 관리하시나요?", 5,
                    List.of(c("cheese", "높은 곳 대신 바닥 스크래처를 여러 개 두는 것도 좋아요!", 90), c("persona", "체중 관리가 제일 중요하대요. 꾸준히 재 보세요!", 300))));

    private Codes c;
    private Seed s;
    private World w;
    private FieldCryptoService crypto;

    @Override
    public void run(Seed seed, World world) {
        s = seed;
        w = world;
        c = s.codes();
        crypto = s.crypto();
        for (P p : POSTS) {
            general(p);
        }
        lostFound();
        lostMissing();
        lateComments();
        personaRecruit();
        cheeseRecruit();
    }

    private World.Member member(String key) {
        return w.members.stream().filter(m -> m.key().equals(key)).findFirst().orElse(null);
    }

    // ---- 일반 글

    private Row general(P p) {
        World.Member author = member(p.key());
        if (author == null) {
            SeedWarnings.add("글쓴이 " + p.key() + "가 없어 글 '" + p.title() + "'을 건너뜀");
            return null;
        }
        LocalDateTime at = s.today().minusDays(p.daysAgo()).atTime(p.hour(), s.between(0, 59));
        if (at.isBefore(author.joinedAt())) {
            at = author.joinedAt().plusHours(s.between(2, 30));
        }
        if (at.isAfter(s.now())) {
            at = s.now().minusMinutes(s.between(30, 120));
        }
        Row post = post(author, p.bbs(), p.title(), p.content(), at, views(at));
        comments(post, author, p.comments(), at);
        likes(post, author, p.likes(), at);
        return post;
    }

    private Row post(World.Member author, String bbs, String title, String content, LocalDateTime at, int views) {
        Row post = s.store().add("PST", r -> r.set("MBR_SN", author.sn()).set("BBS_CD", c.of("CMUG01", bbs))
                .set("PST_STTS_CD", c.of("CMUG02", "정상")).set("PST_TTL", title).set("PST_CN", content).set("INQ_CNT", views)
                .set("RCMDTN_CNT", 0).set("REG_DT", at).by(author.sn()));
        s.store().add("PST_STTS_HSTRY", r -> r.set("PST_SN", post.sn()).set("MBR_SN", author.sn())
                .set("AFTR_STTS_CD", c.of("CMUG02", "정상")).set("REG_DT", at));
        w.posts.add(new World.Post(post.sn(), author.sn(), bbs, at));
        return post;
    }

    /** 오래된 글일수록 조회수가 많다 */
    private int views(LocalDateTime at) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(at.toLocalDate(), s.today());
        return (int) Math.min(420, 12 + days * s.between(2, 5) + s.between(0, 20));
    }

    private void comments(Row post, World.Member author, List<C> list, LocalDateTime postedAt) {
        List<Row> made = new ArrayList<>();
        for (C cm : list) {
            World.Member writer = member(cm.key());
            LocalDateTime at = postedAt.plusMinutes(cm.minutes());
            Row parent = cm.reply() >= 0 && cm.reply() < made.size() ? made.get(cm.reply()) : null;
            if (writer == null || at.isBefore(writer.joinedAt()) || at.isAfter(s.now()) || cm.reply() >= 0 && parent == null) {
                made.add(null);
                continue;
            }
            Row comment = s.store().add("CMNT", r -> r.set("PRNT_CMNT_SN", parent == null ? null : parent.sn())
                    .set("RPLY_TRGT_CMNT_SN", parent == null ? null : parent.sn()).set("PST_SN", post.sn())
                    .set("MBR_SN", writer.sn()).set("CMNT_CN", cm.text()).set("REG_DT", at));
            made.add(comment);
            if (writer.sn() != author.sn()) {
                w.alarms.add(new World.Alarm(author.sn(), "커뮤니티", "내 글에 댓글이 달렸어요", writer.nickname() + ": " + cm.text(),
                        String.format(POST_PATH, post.sn()), at));
            }
        }
    }

    private void likes(Row post, World.Member author, int count, LocalDateTime postedAt) {
        List<World.Member> others = w.writers().stream().filter(m -> m.sn() != author.sn()).toList();
        int n = 0;
        for (World.Member m : s.sample(others, others.size())) {
            if (n >= count) {
                break;
            }
            LocalDateTime from = m.joinedAt().isAfter(postedAt) ? m.joinedAt() : postedAt;
            if (from.isAfter(s.now().minusMinutes(5))) {
                continue;
            }
            LocalDateTime at = s.soonAfter(from, 60 * 24 * 3);
            s.store().add("PST_RCMDTN", r -> r.set("PST_SN", post.sn()).set("MBR_SN", m.sn()).set("REG_DT", at));
            n++;
        }
        post.set("RCMDTN_CNT", n);
    }

    /** 최근 가입했거나 탈퇴한 테스트 계정의 댓글 : 가입 뒤 몇 분에 어느 글(제목)에 달았는지 */
    private record Late(String key, String postTitle, String text, int minutesAfterJoin) {
    }

    private static final List<Late> LATE = List.of(
            new Late("t201", "건지산 산책로 추천해요", "쿠키랑 지난 주말에 다녀왔는데 그늘이 많아서 정말 좋았어요", 60 * 24 * 3),
            new Late("t202", "고양이 물 많이 먹이는 방법", "얕은 그릇으로 바꿨더니 떡볶이가 물을 더 잘 마셔요!", 60 * 24 * 5),
            new Late("t205", "건지산 산책로 추천해요", "해피 데리고 다녀왔어요. 진돗개도 좋아하네요", 60 * 24 * 4),
            new Late("t206", "고양이 모래 5종 써 본 후기", "모모는 카사바로 정착했어요. 먼지가 적어서 좋아요", 60 * 24 * 2),
            new Late("t207", "노령견 슬개골 관리 3주 해 보고 달라진 점", "저희 초코도 디스크 때문에 계단 쓰는데 정말 도움 돼요.", 60 * 24 * 3),
            new Late("t212", "나비가 처음으로 무릎에 올라왔어요", "루루도 무릎에 올라오기까지 1년 걸렸어요 ㅎㅎ", 60 * 20),
            new Late("t213", "구름이 첫 예방접종 다녀왔어요", "포도도 다음 주에 첫 접종이에요!", 60 * 5),
            new Late("t214", "노령 포메 기관지 협착, 겨울 대비 이렇게 해요", "밤이도 포메라 미리 하네스 샀어요", 60 * 3),new Late("t208", "단추가 또 산책 중에 드러누웠어요 ㅋㅋ", "비글도 산책 거부 장난 아니에요 ㅋㅋ", 60 * 54),
            new Late("t210", "솜이 10살 생일이에요", "솜이 생일 축하해요! 오래오래 건강하길 🎂", 60 * 30),
            new Late("t211", "치즈 다이어트 시작합니다", "저희 미미도 살 빼야 하는데 같이 해요 ㅎㅎ", 60 * 52),
            new Late("t215", "보리 13.3kg 찍었어요!!", "저도 오늘부터 율무 체중 기록 시작했어요!", 40),
            new Late("t216", "오늘 아침 송천동 안개 산책", "봄이도 오늘부터 기록 시작해요 :)", 35));

    private void lateComments() {
        for (Late late : LATE) {
            World.Member writer = member(late.key());
            Row post = s.store().rows("PST").stream().filter(p -> late.postTitle().equals(p.get("PST_TTL"))).findFirst().orElse(null);
            if (writer == null || post == null) {
                continue;
            }
            LocalDateTime at = writer.joinedAt().plusMinutes(late.minutesAfterJoin());
            if (writer.withdrawn() && !at.isBefore(writer.leftAt())) {
                at = writer.leftAt().minusMinutes(30);
            }
            if (at.isAfter(s.now()) || at.isBefore(writer.joinedAt()) || at.isBefore((LocalDateTime) post.get("REG_DT"))) {
                SeedWarnings.add("댓글 시각이 맞지 않아 " + late.key() + "의 댓글을 건너뜀");
                continue;
            }
            LocalDateTime when = at;
            s.store().add("CMNT", r -> r.set("PST_SN", post.sn()).set("MBR_SN", writer.sn()).set("CMNT_CN", late.text()).set("REG_DT", when));
            long authorSn = (Long) post.get("MBR_SN");
            w.alarms.add(new World.Alarm(authorSn, "커뮤니티", "내 글에 댓글이 달렸어요", writer.nickname() + ": " + late.text(),
                    String.format(POST_PATH, post.sn()), when));
        }
    }

    // ---- 실종센터

    /**
     * 주인공이 덕진공원에서 콩이를 잠깐 잃어버렸다가 3시간 만에 찾았다
     */
    private void lostFound() {
        World.Member persona = w.persona();
        World.Pet kong = w.pet("kong");
        Masters.Region region = s.masters().region(People.SGG, persona.emdNm());
        LocalDateTime seenAt = s.today().minusDays(41).atTime(17, 20);
        LocalDateTime postedAt = seenAt.plusMinutes(25);
        Row post = post(persona, "실종센터", "덕진공원에서 말티즈 콩이를 찾습니다",
                "오늘 오후 5시 20분쯤 덕진공원 연못 산책로에서 목줄이 풀려 잃어버렸어요.\n\n하얀 말티즈, 11살, 3.4kg이에요. 왼쪽 뒷다리를 조금 절어요. 겁이 많아서 부르면 도망갈 수 있으니 보시면 연락 부탁드립니다.",
                postedAt, 210);
        LocalDateTime foundAt = postedAt.plusHours(3).plusMinutes(10);
        Row lost = s.store().add("LOST_TPOF", r -> r.set("PST_SN", post.sn()).set("RGN_SN", region.sn())
                .set("PET_SE_CD", c.of("PETG01", "강아지")).set("LOST_TPOF_STTS_CD", c.of("CMUG03", "찾았어요")).set("PET_NM", kong.name())
                .set("COLR_NM", "흰색").set("CNPL_ENC", crypto.encrypt("01000001234")).set("WTNS_PLC_LAT", s.decimal(35.8466, 10))
                .set("WTNS_PLC_LOT", s.decimal(127.1213, 10)).set("LAST_WTNS_DT", seenAt).set("EXPRY_CRTR_DT", seenAt.plusDays(30))
                .set("DSCV_SWTC_DT", foundAt).set("WTNS_PLC_NM", "덕진공원 연못 산책로").yn("PRVC_RLS_AGRE_YN", true)
                .set("VRTY_SN", kong.vrtySn()).set("CHRC_CN", "빨간 하네스, 왼쪽 뒷다리를 조금 절어요").set("REG_DT", postedAt));
        lostHistory(lost.sn(), persona.sn(), null, "실종", postedAt);
        lostHistory(lost.sn(), persona.sn(), "실종", "찾았어요", foundAt);
        comments(post, persona, List.of(c("boriDad", "공원 북쪽 입구 쪽 찾아볼게", 8), c("songcheon", "덕진공원 근처라 저도 단추랑 한 바퀴 돌아볼게요!", 40),
                c("inhuPom", "꼭 찾으시길 바랄게요ㅠㅠ", 95)), postedAt);
        String template = s.masters().stringSetting("DSCV_AUTO_CMNT_CN");
        String found = (template == null ? "🎉 [펫이름]가 무사히 가족 품으로 돌아왔어요!" : template).replace("[펫이름]", kong.name());
        s.store().add("CMNT", r -> r.set("PST_SN", post.sn()).set("MBR_SN", persona.sn()).set("CMNT_CN", found)
                .yn("SYS_AUTO_CRT_YN", true).set("REG_DT", foundAt));
        comments(post, persona, List.of(c("songcheon", "다행이에요!! 놀랐겠어요", 200), c("persona", "도와주신 분들 정말 감사합니다. 공원 화장실 뒤에 숨어 있었어요 ㅠㅠ", 215)),
                postedAt);
        likes(post, persona, 8, postedAt);
    }

    /**
     * 수줍은집사의 나비가 창문으로 나가 실종 중
     */
    private void lostMissing() {
        World.Member owner = member("shy");
        if (owner == null) {
            return;
        }
        World.Pet nabi = w.pet("nabi");
        Masters.Region region = s.masters().region(People.SGG, owner.emdNm());
        LocalDateTime seenAt = s.today().minusDays(4).atTime(6, 40);
        LocalDateTime postedAt = seenAt.plusHours(2);
        Row post = post(owner, "실종센터", "진북동에서 러시안 블루 나비를 찾아요",
                "새벽에 방충망이 열린 틈으로 나간 것 같아요.\n\n회색 러시안 블루, 5살 암컷이고 4kg 정도예요. 겁이 많아서 차 밑이나 화단에 숨어 있을 수 있어요. 진북동 우성아파트 근처에서 보시면 꼭 연락 주세요.",
                postedAt, 160);
        int days = s.masters().intSetting("LOST_TPOF_EXPRY_CRTR_DAY_CNT");
        Row lost = s.store().add("LOST_TPOF", r -> r.set("PST_SN", post.sn()).set("RGN_SN", region.sn())
                .set("PET_SE_CD", c.of("PETG01", "고양이")).set("LOST_TPOF_STTS_CD", c.of("CMUG03", "실종")).set("PET_NM", nabi.name())
                .set("COLR_NM", "회색").set("CNPL_ENC", crypto.encrypt("01000005678")).set("WTNS_PLC_LAT", s.decimal(35.8299, 10))
                .set("WTNS_PLC_LOT", s.decimal(127.1361, 10)).set("LAST_WTNS_DT", seenAt).set("EXPRY_CRTR_DT", seenAt.plusDays(days))
                .set("WTNS_PLC_NM", "진북동 아파트 단지 화단").yn("PRVC_RLS_AGRE_YN", true).set("VRTY_SN", nabi.vrtySn())
                .set("CHRC_CN", "목에 하늘색 방울 목걸이").set("REG_DT", postedAt));
        lostHistory(lost.sn(), owner.sn(), null, "실종", postedAt);
        comments(post, owner, List.of(c("cheese", "고양이는 멀리 안 간다고 하더라고요. 밤에 조용할 때 이름 불러 보세요. 꼭 찾길 바라요", 50),
                c("persona", "진북동 근처 지날 때 잘 보고 다닐게요!", 180), c("seosin", "사료랑 화장실 모래를 현관 앞에 두면 냄새 맡고 온대요", 400)), postedAt);
        likes(post, owner, 11, postedAt);
    }

    private void lostHistory(long lostSn, Long mbrSn, String before, String after, LocalDateTime at) {
        s.store().add("LOST_TPOF_STTS_HSTRY", r -> r.set("LOST_TPOF_SN", lostSn).set("MBR_SN", mbrSn)
                .set("BFR_STTS_CD", before == null ? null : c.of("CMUG03", before)).set("AFTR_STTS_CD", c.of("CMUG03", after))
                .set("REG_DT", at));
    }

    // ---- 우리동네 펫돌봄

    /**
     * 주인공 : 12월 출장 기간 보리 산책 모집. 단추아빠가 채팅으로 문의 중
     */
    private void personaRecruit() {
        World.Member persona = w.persona();
        World.Pet bori = w.pet("bori");
        LocalDateTime postedAt = s.today().minusDays(2).atTime(21, 5);
        LocalDateTime begin = s.today().plusDays(14).atTime(8, 0);
        Row post = post(persona, "우리동네 펫돌봄", "12월 둘째 주 사흘 동안 코기 보리 산책 도와주실 분",
                "출장 때문에 사흘 동안 아침·저녁 산책을 도와주실 분을 찾아요.\n\n- 웰시코기 보리(암컷, 3살, 13kg)\n- 하루 두 번, 한 번에 40분 정도\n- 다이어트 중이라 간식은 주지 말아 주세요\n- 사람·강아지 모두 좋아하고 줄 당김은 조금 있어요\n\n덕진동 근처 사시는 분이면 좋겠어요.",
                postedAt, 46);
        Row rcrt = recruit(persona, post, "덕진동", "시급", 13_000, "전주시 덕진구 덕진동", "전주시 덕진구 덕진동 행복아파트 103동 1203호",
                begin, begin.plusDays(2).withHour(20), true, 35.8461, 127.1225, postedAt, List.of("산책", "방문돌봄"));
        s.store().add("RCRT_PET_KND", r -> r.set("PET_PRTC_RCRT_SN", rcrt.sn()).set("PET_SE_CD", c.of("PETG01", "강아지"))
                .set("PET_BDWG", s.decimal(13.3, 1)).set("VRTY_SN", bori.vrtySn()).set("REG_DT", postedAt));
        likes(post, persona, 3, postedAt);

        World.Member walker = member("songcheon");
        LocalDateTime chatAt = postedAt.plusHours(13);
        chat(post, rcrt, persona, walker, chatAt, List.of(
                new String[] { "walker", "안녕하세요! 송천동 사는 단추 보호자예요. 보리 산책 제가 도와드려도 될까요?" },
                new String[] { "author", "안녕하세요~ 글 봐 주셔서 감사해요! 시바 키우시는 분이면 든든하네요 ㅎㅎ" },
                new String[] { "walker", "출근 전 7시 반, 퇴근 후 7시쯤 가능해요. 단추랑 같이 걸어도 괜찮을까요?" },
                new String[] { "author", "보리가 강아지 친구 좋아해서 오히려 좋아요! 줄 당김만 조금 조심해 주세요" },
                new String[] { "walker", "넵 하네스 잘 채워서 갈게요. 시작 전에 한 번 만나서 인사시키면 좋을 것 같아요" },
                new String[] { "author", "좋아요! 이번 주말 오전에 덕진공원 어떠세요?" }), true);
    }

    /**
     * 치즈집사 : 주말 1박 고양이 방문 돌봄 모집(문의 아직 없음)
     */
    private void cheeseRecruit() {
        World.Member owner = member("cheese");
        if (owner == null) {
            return;
        }
        LocalDateTime postedAt = s.today().minusDays(10).atTime(22, 30);
        LocalDateTime begin = s.today().plusDays(9).atTime(10, 0);
        Row post = post(owner, "우리동네 펫돌봄", "주말 1박 2일 고양이 두 마리 방문 돌봄 구해요",
                "가족 행사로 1박 2일 집을 비워요. 하루 한 번 방문해서 밥·물 갈아 주고 화장실 치워 주실 분 찾아요.\n\n치즈(5.6kg, 다이어트 중)·망고(4.8kg) 두 마리이고 사람 좋아하는 편이에요. 방문 30분 정도면 충분해요.",
                postedAt, 58);
        Row rcrt = recruit(owner, post, "금암동", "건당", 25_000, "전주시 덕진구 금암동", "전주시 덕진구 금암동 금암빌라 2층",
                begin, begin.plusDays(1).withHour(18), false, 35.8394, 127.1324, postedAt, List.of("방문돌봄"));
        s.store().add("RCRT_PET_KND", r -> r.set("PET_PRTC_RCRT_SN", rcrt.sn()).set("PET_SE_CD", c.of("PETG01", "고양이"))
                .set("PET_BDWG", s.decimal(5.6, 1)).set("VRTY_SN", w.pet("cheeseCat").vrtySn()).set("REG_DT", postedAt));
        likes(post, owner, 4, postedAt);
    }

    private Row recruit(World.Member author, Row post, String emdNm, String unit, int amount, String publicRegion, String address,
            LocalDateTime begin, LocalDateTime end, boolean negotiable, double lat, double lot, LocalDateTime at, List<String> shapes) {
        Masters.Region region = s.masters().region(People.SGG, emdNm);
        Row rcrt = s.store().add("PET_PRTC_RCRT", r -> r.set("PST_SN", post.sn()).set("RGN_SN", region.sn())
                .set("RCRT_STTS_CD", c.of("PCAG01", "모집중")).set("CST_UNIT_CD", c.of("PCAG02", unit)).set("CST_AMT", amount)
                .set("RLS_RGN_NM", publicRegion).set("PRTC_PLC_DADDR_ENC", crypto.encrypt(address)).set("PRTC_BGNG_DT", begin)
                .set("PRTC_END_DT", end).yn("SCHDL_CNSLTN_PSBLTY_YN", negotiable).yn("UTZTN_RUL_AGRE_YN", true)
                .set("PRTC_PLC_LAT", s.decimal(lat, 10)).set("PRTC_PLC_LOT", s.decimal(lot, 10)).set("REG_DT", at));
        s.store().add("PET_PRTC_RCRT_STTS_HSTRY", r -> r.set("PET_PRTC_RCRT_SN", rcrt.sn()).set("MBR_SN", author.sn())
                .set("AFTR_STTS_CD", c.of("PCAG01", "모집중")).set("REG_DT", at));
        for (String shape : shapes) {
            s.store().add("RCRT_PRTC_SHP", r -> r.set("PET_PRTC_RCRT_SN", rcrt.sn()).set("PRTC_SHP_CD", c.of("PCAG03", shape))
                    .set("REG_DT", at));
        }
        return rcrt;
    }

    /**
     * 채팅방(지원자가 연다)·참여자·텍스트 메시지. 글쓴이는 마지막 메시지까지 읽었다
     * - requestMatch : 대화 뒤 보호자(글쓴이)가 매칭을 요청한다(CareMatchService.request와 같게 수락 대기·조건 1판·상태 이력·시스템 메시지·제공자 알림)
     */
    private void chat(Row post, Row rcrt, World.Member author, World.Member applicant, LocalDateTime at, List<String[]> lines,
            boolean requestMatch) {
        Row room = s.store().add("OTO_CHAT_ROOM", r -> r.set("PST_SN", post.sn()).set("CTPT_MBR_SN", applicant.sn())
                .set("CHAT_ROOM_STTS_CD", c.of("CHTG01", "활성")).set("REG_DT", at));
        s.store().add("OTO_CHAT_ROOM_STTS_HSTRY", r -> r.set("OTO_CHAT_ROOM_SN", room.sn()).set("MBR_SN", applicant.sn())
                .set("AFTR_STTS_CD", c.of("CHTG01", "활성")).set("REG_DT", at));
        LocalDateTime t = at;
        int seq = 0;
        for (String[] line : lines) {
            long sender = "author".equals(line[0]) ? author.sn() : applicant.sn();
            int msgSeq = ++seq;
            LocalDateTime when = t;
            s.store().add("CHAT_MSG", r -> r.set("OTO_CHAT_ROOM_SN", room.sn()).set("MBR_SN", sender)
                    .set("MSG_TYPE_CD", c.of("CHTG03", "텍스트")).set("MSG_SEQ", msgSeq).set("MSG_CN_ENC", crypto.encrypt(line[1]))
                    .set("IDMPT_DMND_ID", s.uuid()).set("REG_DT", when));
            t = t.plusMinutes(s.between(2, 25));
        }
        int applicantRead = seq;
        if (requestMatch) {
            LocalDateTime requestedAt = t.plusMinutes(s.between(10, 30));
            Row match = s.store().add("PET_PRTC_MTCH", r -> r.set("PET_PRTC_RCRT_SN", rcrt.sn()).set("OTO_CHAT_ROOM_SN", room.sn())
                    .set("MTCH_DLNG_STTS_CD", c.of("PCAG05", "수락 대기")).set("IDMPT_DMND_ID", s.uuid()).set("REG_DT", requestedAt));
            Row condition = s.store().add("MTCH_CND_VER", r -> r.set("PET_PRTC_MTCH_SN", match.sn()).set("MBR_SN", author.sn())
                    .set("CST_UNIT_CD", rcrt.get("CST_UNIT_CD")).set("CND_VER_NO", 1).set("ACRD_CST_AMT", rcrt.get("CST_AMT"))
                    .set("ACRD_PRTC_CN", post.get("PST_CN")).set("ACRD_PRTC_BGNG_DT", rcrt.get("PRTC_BGNG_DT"))
                    .set("ACRD_PRTC_END_DT", rcrt.get("PRTC_END_DT")).set("SCHDL_CNSLTN_PSBLTY_YN", rcrt.get("SCHDL_CNSLTN_PSBLTY_YN"))
                    .set("REG_DT", requestedAt));
            s.store().add("PET_PRTC_MTCH_STTS_HSTRY", r -> r.set("PET_PRTC_MTCH_SN", match.sn()).set("MBR_SN", author.sn())
                    .set("MTCH_CND_VER_SN", condition.sn()).set("AFTR_STTS_CD", c.of("PCAG05", "수락 대기")).set("REG_DT", requestedAt));
            int sysSeq = ++seq;
            s.store().add("CHAT_MSG", r -> r.set("OTO_CHAT_ROOM_SN", room.sn()).set("MSG_TYPE_CD", c.of("CHTG03", "시스템"))
                    .set("MSG_SEQ", sysSeq).set("MSG_CN_ENC", crypto.encrypt("매칭을 요청했어요.")).set("IDMPT_DMND_ID", "SYS-" + s.uuid())
                    .set("REG_DT", requestedAt));
            w.alarms.add(new World.Alarm(applicant.sn(), "펫돌봄 거래 상태", "펫돌봄 거래", "매칭 요청이 왔어요. 조건을 확인하고 수락해 주세요.",
                    String.format(CHAT_PATH, room.sn()), requestedAt));
            t = requestedAt;
        }
        int last = seq;
        LocalDateTime lastAt = t;
        participant(room.sn(), author.sn(), last, lastAt, at);
        participant(room.sn(), applicant.sn(), applicantRead - 1, lastAt.minusMinutes(1), at);
        w.alarms.add(new World.Alarm(author.sn(), "펫돌봄 거래 상태", "펫돌봄 문의가 왔어요", applicant.nickname() + "님이 채팅을 보냈어요.",
                String.format(CHAT_PATH, room.sn()), at));
    }

    private void participant(long roomSn, long mbrSn, int lastSeq, LocalDateTime lastAt, LocalDateTime joinedAt) {
        Row p = s.store().add("CHAT_PRTPNT", r -> r.set("OTO_CHAT_ROOM_SN", roomSn).set("MBR_SN", mbrSn)
                .set("PTCP_STTS_CD", c.of("CHTG02", "참여 중")).set("LAST_PRSL_MSG_SEQ", lastSeq).set("LAST_PRSL_DT", lastAt)
                .set("REG_DT", joinedAt));
        s.store().add("CHAT_PRTPNT_STTS_HSTRY", r -> r.set("CHAT_PRTPNT_SN", p.sn()).set("MBR_SN", mbrSn)
                .set("AFTR_STTS_CD", c.of("CHTG02", "참여 중")).set("REG_DT", joinedAt));
    }

    static String lostPath(long pstSn) {
        return String.format(LOST_PATH, pstSn);
    }
}
