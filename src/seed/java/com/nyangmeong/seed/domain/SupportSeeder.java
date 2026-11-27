package com.nyangmeong.seed.domain;

import java.time.LocalDateTime;
import java.util.List;

import com.nyangmeong.seed.core.Codes;
import com.nyangmeong.seed.core.Masters;
import com.nyangmeong.seed.core.Row;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.SeedWarnings;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.World;

/**
 * [1:1 문의]
 * - 주인공 : 답변 완료 1건, 접수(답변 대기) 1건. 엑스트라 몇 건
 * - 답변은 남긴 관리자(가장 낮은 번호)가 단다. 답변이 달리면 문의한 회원에게 알림
 * - 사진 첨부(INQRY_ATCH_PHOTO)는 넣지 않는다
 */
public class SupportSeeder implements Seeder {

    private static final String PATH = "/inquiries/%d";

    private record Q(String key, String type, int daysAgo, String title, String content, String answer, int answerHours) {
    }

    private static final List<Q> QUESTIONS = List.of(
            new Q("persona", "건강기록 문의", 50, "검사 수치 그래프에서 SDMA가 안 보여요",
                    "두부 혈액검사 결과를 넣었는데 BUN, CREA는 그래프에 나오고 SDMA는 목록에만 있어요. 직접 입력으로 넣어서 그런 걸까요?",
                    "안녕하세요, 냥멍케어입니다. 문의 주셔서 감사합니다.\n\n직접 입력으로 등록한 항목은 그래프에서 제외되고 있었어요. 이번 업데이트로 검사 항목 목록에 SDMA를 추가했으니, 다음 기록부터는 목록에서 SDMA를 골라 주시면 그래프에 함께 보여요.\n\n불편을 드려 죄송합니다.",
                    20),
            new Q("persona", "포인트·결제·가계부", 1, "공동양육자가 쓴 지출도 월별 통계에 합쳐지나요?",
                    "보리를 남편과 같이 키우고 있는데, 남편이 가계부에 보리 사료값을 넣으면 제 월별 통계에도 보이는지 궁금해요.", null, 0),
            new Q("cheese", "커뮤니티", 34, "글을 수정하면 좋아요가 사라지나요?",
                    "모래 후기 글을 조금 고치고 싶은데 수정하면 좋아요랑 댓글이 그대로 남는지 궁금합니다.",
                    "안녕하세요, 냥멍케어입니다.\n\n글을 수정해도 좋아요와 댓글은 그대로 유지돼요. 편하게 수정하셔도 됩니다. 감사합니다.", 5),
            new Q("songcheon", "플레이스", 12, "반려견 동반 가능한 카페를 추가하고 싶어요",
                    "송천동에 반려견 동반 되는 카페가 새로 생겼는데 플레이스에 안 나와요. 제보할 수 있는 방법이 있을까요?", null, 0),
            new Q("t207", "계정·로그인", 6, "카카오로 가입했는데 이메일 로그인도 할 수 있나요?",
                    "카카오로 가입했는데 회사 PC에서는 이메일로 로그인하고 싶어서요. 비밀번호를 따로 만들 수 있을까요?",
                    "안녕하세요, 냥멍케어입니다.\n\nSNS로 가입한 계정은 비밀번호가 없어 이메일 로그인을 지원하지 않아요. 같은 카카오 계정으로 로그인해 주세요. 감사합니다.",
                    26),
            new Q("madeleine", "건강기록 문의", 3, "예방접종 기록을 한꺼번에 넣을 수 있나요?",
                    "마들렌 접종 기록이 지난 2년치가 있는데 하나씩 넣으려니 너무 번거로워요. 한꺼번에 등록하는 방법이 따로 있을까요?",
                    null, 0),
            new Q("punch", "플레이스", 2, "장소 즐겨찾기가 목록에서 사라졌어요",
                    "어제까지 즐겨찾기해 둔 동물병원이 오늘 보니까 목록에 없어요. 제가 뭘 잘못 누른 걸까요?",
                    null, 0),
            new Q("rookie", "계정·로그인", 1, "비밀번호를 바꾸고 싶은데 어디서 하나요?",
                    "고양이를 처음 키우면서 가입했는데 비밀번호를 너무 단순하게 해 둬서 바꾸고 싶어요. 설정에서 못 찾겠어요.",
                    null, 0),
            new Q("dotori", "픽셀 캐릭터", 5, "픽셀 캐릭터 옷은 언제 새로 나오나요?",
                    "캐릭터 꾸미기에서 입힐 수 있는 옷이 몇 개 없어서요. 새 아이템 나오는 시기를 알 수 있을까요?",
                    null, 0),
            new Q("gonggu", "커뮤니티", 7, "제 글이 목록에서 안 보여요",
                    "어제 올린 글이 내 글 목록에는 있는데 커뮤니티 목록에서는 안 보이네요. 확인 부탁드립니다.",
                    null, 0),
            new Q("mocha", "포인트·결제·가계부", 8, "출석 포인트가 두 번 들어간 것 같아요",
                    "오늘 출석체크를 했는데 포인트 내역에 같은 시각으로 두 줄이 보여요. 정상인가요?",
                    null, 0),
            new Q("jjanggu", "포인트·결제·가계부", 26, "가계부에서 지난달 지출을 삭제하면 통계가 바로 바뀌나요?",
                    "잘못 넣은 사료값 항목을 지우려는데 삭제하면 월별 통계에서 바로 빠지는지 알고 싶어요.",
                    "안녕하세요, 냥멍케어입니다.\n\n지출을 삭제하면 월별 통계에서 바로 빠져요. 화면이 그대로라면 새로고침 후 다시 확인해 주세요. 감사합니다.", 4),
            new Q("wdMong", "기타", 30, "앱 이용 중 의견을 남기고 싶어요",
                    "쓰다 보니 아쉬운 점이 몇 가지 있는데 어디에 의견을 남기면 되는지 모르겠어요.",
                    "안녕하세요, 냥멍케어입니다.\n\n의견은 이 1:1 문의로 남겨 주시면 돼요. 주신 내용은 서비스 개선에 참고하겠습니다. 감사합니다.", 18),
            new Q("rude", "커뮤니티", 4, "댓글 신고는 어떻게 하나요?",
                    "어떤 분이 계속 시비 거는 댓글을 다는데 신고 버튼이 어디 있는지 모르겠어요.",
                    "안녕하세요, 냥멍케어입니다.\n\n댓글 오른쪽 위의 점 세 개 메뉴에서 신고하기를 누르시면 돼요. 접수된 신고는 확인 후 처리합니다. 감사합니다.", 5),
            new Q("summer", "픽셀 캐릭터", 18, "캐릭터가 산책 사진처럼 안 나와요",
                    "우리 애 털색이랑 캐릭터 색이 좀 달라 보여요. 다시 만들 수 있나요?",
                    "안녕하세요, 냥멍케어입니다.\n\n캐릭터는 반려동물 정보의 털색을 기준으로 만들어져요. 반려동물 정보에서 털색을 고친 뒤 캐릭터를 다시 만들면 새로 반영됩니다. 감사합니다.", 9),
            new Q("bora", "계정·로그인", 40, "이메일 인증 메일이 안 와요",
                    "가입할 때 인증 메일이 한참 지나도 안 왔어요. 스팸함도 봤는데 없네요.",
                    "안녕하세요, 냥멍케어입니다.\n\n인증 메일이 늦어지는 경우가 있어요. 스팸함 확인 후에도 없으면 인증 메일을 다시 받아 주세요. 계속 오지 않으면 다시 문의 주세요. 감사합니다.", 3));

    @Override
    public void run(Seed s, World w) {
        Codes c = s.codes();
        Masters.KeptMember admin = s.masters().members().stream()
                .filter(m -> "관리자".equals(c.name("USRG02", m.authorityCd())) && "정상".equals(c.name("USRG01", m.statusCd())))
                .findFirst().orElse(null);
        for (Q q : QUESTIONS) {
            World.Member member = w.members.stream().filter(m -> m.key().equals(q.key())).findFirst().orElse(null);
            if (member == null) {
                continue;
            }
            LocalDateTime at = s.today().minusDays(q.daysAgo()).atTime(s.between(10, 22), s.between(0, 59));
            if (at.isBefore(member.joinedAt())) {
                at = member.joinedAt().plusHours(3);
            }
            if (at.isAfter(s.now())) {
                at = s.now().minusHours(2);
            }
            if (member.withdrawn() && !at.isBefore(member.leftAt()) || member.inactiveFrom() != null && !at.isBefore(member.inactiveFrom())) {
                SeedWarnings.add("문의 '" + q.title() + "'가 " + member.key() + "의 탈퇴·휴면 이후 시각이라 건너뜀");
                continue;
            }
            boolean answered = q.answer() != null && admin != null;
            if (q.answer() != null && admin == null) {
                SeedWarnings.add("관리자가 없어 문의 '" + q.title() + "'에 답변을 달지 않음");
            }
            LocalDateTime askedAt = at;
            LocalDateTime answeredAt = answered ? at.plusHours(q.answerHours()) : null;
            Row inquiry = s.store().add("OTO_INQRY", r -> r.set("MBR_SN", member.sn()).set("PRCS_MBR_SN", answered ? admin.sn() : null)
                    .set("INQRY_TYPE_CD", c.of("CSCG02", q.type())).set("INQRY_PRCS_STTS_CD", c.of("CSCG03", answered ? "답변 완료" : "접수"))
                    .set("INQRY_TTL", q.title()).set("INQRY_CN", q.content()).set("ANS_CN", answered ? q.answer() : null)
                    .set("ANS_DT", answeredAt).set("REG_DT", askedAt));
            s.store().add("OTO_INQRY_STTS_HSTRY", r -> r.set("OTO_INQRY_SN", inquiry.sn()).set("MBR_SN", member.sn())
                    .set("AFTR_STTS_CD", c.of("CSCG03", "접수")).set("REG_DT", askedAt));
            if (answered) {
                s.store().add("OTO_INQRY_STTS_HSTRY", r -> r.set("OTO_INQRY_SN", inquiry.sn()).set("MBR_SN", admin.sn())
                        .set("BFR_STTS_CD", c.of("CSCG03", "접수")).set("AFTR_STTS_CD", c.of("CSCG03", "답변 완료")).set("REG_DT", answeredAt));
                if (inquiry.get("MDFCN_DT") == null) {
                    inquiry.set("MDFCN_DT", answeredAt);
                }
                w.alarms.add(new World.Alarm(member.sn(), "1:1 문의 답변", "1:1 문의에 답변이 등록됐어요", q.title(),
                        String.format(PATH, inquiry.sn()), answeredAt));
            }
        }
    }
}
