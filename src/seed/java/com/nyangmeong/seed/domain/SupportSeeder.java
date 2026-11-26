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
                    26));

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
