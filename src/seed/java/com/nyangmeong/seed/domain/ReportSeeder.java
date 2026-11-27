package com.nyangmeong.seed.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.nyangmeong.community.service.ReportService;
import com.nyangmeong.seed.core.Codes;
import com.nyangmeong.seed.core.Row;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.SeedWarnings;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.World;

/**
 * [신고]
 * - CommunitySeeder가 만든 글·댓글 중 정해 둔 것을 신고한다. 대상은 글쓴이 key와 제목(글) 또는 앞부분(댓글)으로 찾는다
 * - 접수 8건(처리 전), 처리완료·반려 6건, 신고가 쌓여 자동 처리된 것 3건(글 블라인드 2, 댓글 삭제 1)
 * - 처리는 남긴 관리자(World.adminSn)가 한다. 자동 처리는 처리자 없이 이력만 남긴다
 * - 신고자는 활동 중인 일반 회원에서 뽑고 같은 대상을 두 번 신고하지 않는다. 채팅 상대 신고는 넣지 않는다
 */
public class ReportSeeder implements Seeder {

    /** 신고 대상 : 글(comment=false)은 match가 제목, 댓글은 match로 시작하는 댓글(빈 문자열이면 아무 댓글). 댓글이 없으면 fallbackTitle 글로 대신 */
    record Target(String authorKey, boolean comment, String match, String fallbackTitle) {
    }

    /** 처리 결과 */
    enum Outcome {
        /** 접수 상태로 둔다 */
        PENDING,
        /** 처리완료 · 게시물 블라인드 */
        BLIND,
        /** 처리완료 · 작성자 정지(처리 시각은 작성자의 inactiveFrom) */
        SUSPEND,
        /** 처리완료 · 유지 */
        KEEP,
        /** 반려 */
        REJECT,
        /** 신고 누적으로 글 자동 블라인드(신고는 모두 접수) */
        AUTO_BLIND,
        /** 신고 누적으로 댓글 자동 삭제(신고는 모두 접수) */
        AUTO_DELETE
    }

    /** 신고 계획 : reporters가 0이면 BLIND_DCLR_ACML_CNT명 */
    record Plan(Target target, String reason, int reporters, String detail, Outcome outcome) {
    }

    private static final String SPAM = "스팸·광고성";
    private static final String ABUSE = "욕설·비방";
    private static final String PRIVACY = "개인정보 노출";
    private static final String FRAUD = "사기·허위 정보";
    private static final String ETC = "기타";

    private static Target post(String key, String title) {
        return new Target(key, false, title, title);
    }

    static final List<Plan> PLANS = List.of(
            // 접수 (처리 전)
            new Plan(post("spamAd", "사료 반값 이벤트 선착순 20명"), SPAM, 2, "선착순이라면서 외부 링크로 유도하는 광고예요", Outcome.PENDING),
            new Plan(post("gonggu", "사료 공동구매 총대합니다"), SPAM, 1, null, Outcome.PENDING),
            new Plan(post("dotori", "도토리 첫 산책 성공했어요"), PRIVACY, 1, "본문에 전화번호가 그대로 적혀 있어요", Outcome.PENDING),
            new Plan(post("dawn", "새벽 두 시의 우다다"), ETC, 1, "같은 말을 반복하는 글 같아요", Outcome.PENDING),
            new Plan(post("platy", "치료비 후원 부탁드립니다"), FRAUD, 2, "사진 출처가 불분명하고 계좌만 반복해서 올려요", Outcome.PENDING),
            new Plan(post("hairball", "메인쿤 털뭉 6kg 돌파"), ABUSE, 1, null, Outcome.PENDING),
            // 처리완료·반려
            new Plan(post("spamAd", "수제간식 무료 체험단 모집(링크 클릭)"), SPAM, 2, "링크를 누르면 결제 페이지로 넘어가요", Outcome.BLIND),
            new Plan(new Target("rude", true, "개를 왜 키우는지", "비글 산책은 코가 다 한다"), ABUSE, 1, "다른 회원에게 모욕적인 말을 했어요", Outcome.SUSPEND),
            new Plan(post("sarang", "동물병원 비용 비교해 봤어요"), PRIVACY, 1, null, Outcome.REJECT),
            new Plan(post("haru", "대형견 여름 산책 시간대 정리"), ABUSE, 1, null, Outcome.REJECT),
            new Plan(post("mungchi", "노견 시츄 건강검진 결과 공유해요"), FRAUD, 1, "검진 결과가 과장된 것 같아요", Outcome.KEEP),
            // 신고 누적 자동 처리
            new Plan(post("spamAd", "[공동구매] 관절 영양제 최저가 모집합니다"), SPAM, 0, null, Outcome.AUTO_BLIND),
            new Plan(post("rude", "요즘 집사들 수준이 왜 이래요"), ABUSE, 0, null, Outcome.AUTO_BLIND),
            new Plan(new Target("rude", true, "고양이 키울 자격도 없네요", "고양이 두 마리 화장실은 몇 개가 적당할까요"), ABUSE, 0, null,
                    Outcome.AUTO_DELETE));

    @Override
    public void run(Seed s, World w) {
        if (w.adminSn == null) {
            SeedWarnings.add("남긴 관리자가 없어 신고를 만들지 않음");
            return;
        }
        int autoCount = s.masters().intSetting("BLIND_DCLR_ACML_CNT");
        for (Plan plan : PLANS) {
            report(s, w, plan, plan.reporters() > 0 ? plan.reporters() : autoCount);
        }
    }

    private void report(Seed s, World w, Plan plan, int count) {
        Codes c = s.codes();
        World.Member author = w.members.stream().filter(m -> m.key().equals(plan.target().authorKey())).findFirst().orElse(null);
        if (author == null) {
            SeedWarnings.add("신고 대상 작성자 " + plan.target().authorKey() + "이 없어 신고를 건너뜀");
            return;
        }
        Row target = find(s, author, plan.target(), false);
        boolean onComment = plan.target().comment() && target != null;
        if (target == null && plan.target().comment()) {
            target = find(s, author, plan.target(), true);
            SeedWarnings.add("신고할 댓글 '" + plan.target().match() + "'이 없어 글 '" + plan.target().fallbackTitle() + "'을 신고함");
        }
        if (target == null) {
            SeedWarnings.add("신고 대상 '" + plan.target().match() + "'(" + author.key() + ")을 찾지 못해 신고를 건너뜀");
            return;
        }
        // 댓글 대신 글로 바꿨으면 자동 삭제는 할 수 없으므로 글 자동 블라인드로 바꾸지 않고 접수로 둔다
        Outcome outcome = plan.outcome();
        if (!onComment && plan.target().comment() && (outcome == Outcome.AUTO_DELETE || outcome == Outcome.SUSPEND)) {
            outcome = Outcome.PENDING;
        }
        boolean isPost = !onComment;
        LocalDateTime targetAt = (LocalDateTime) target.get("REG_DT");
        LocalDateTime earliest = targetAt.plusMinutes(20);

        // 처리 시각이 정해진 것(작성자 정지)은 그 시각보다 앞에 신고한다
        LocalDateTime fixedProcessAt = null;
        if (outcome == Outcome.SUSPEND) {
            fixedProcessAt = author.inactiveFrom();
            if (fixedProcessAt == null || fixedProcessAt.isBefore(earliest.plusMinutes(10))) {
                SeedWarnings.add("신고 대상 작성자 " + author.key() + "의 정지 시각이 신고 시각과 맞지 않아 신고 뒤로 잡음");
                fixedProcessAt = null;
            }
        }
        LocalDateTime latest = earliest.plusDays(2).minusMinutes(20);
        if (latest.isAfter(s.now())) {
            latest = s.now();
        }
        if (fixedProcessAt != null && latest.isAfter(fixedProcessAt.minusMinutes(10))) {
            latest = fixedProcessAt.minusMinutes(10);
        }

        final Row targetRow = target;
        List<World.Member> pool = w.members.stream()
                .filter(m -> !m.persona() && m.activity() != World.Activity.NONE && !m.withdrawn() && m.inactiveFrom() == null)
                .filter(m -> m.sn() != author.sn() && m.joinedAt().isBefore(earliest)).toList();
        List<World.Member> reporters = s.sample(pool, count);
        if (reporters.size() < count) {
            SeedWarnings.add("신고자가 모자라 '" + plan.target().match() + "' 신고를 " + reporters.size() + "건만 만듦");
        }
        if (reporters.isEmpty()) {
            return;
        }
        List<LocalDateTime> times = new ArrayList<>();
        long span = Math.max(0, java.time.Duration.between(earliest, latest).toMinutes());
        for (int i = 0; i < reporters.size(); i++) {
            times.add(earliest.plusMinutes(span == 0 ? 0 : s.between(0, (int) Math.min(span, Integer.MAX_VALUE))));
        }
        times.sort(Comparator.naturalOrder());
        LocalDateTime lastAt = times.get(times.size() - 1);

        LocalDateTime processAt = null;
        if (outcome == Outcome.BLIND && plan.target().authorKey().equals("spamAd")) {
            processAt = s.now().minusDays(18);
        } else if (outcome == Outcome.BLIND || outcome == Outcome.KEEP || outcome == Outcome.REJECT) {
            processAt = lastAt.plusMinutes(s.between(180, 2400));
        } else if (outcome == Outcome.SUSPEND) {
            processAt = fixedProcessAt != null ? fixedProcessAt : lastAt.plusHours(6);
        }
        if (processAt != null && processAt.isBefore(lastAt.plusMinutes(5))) {
            processAt = lastAt.plusMinutes(5);
        }
        if (processAt != null && processAt.isAfter(s.now())) {
            processAt = s.now();
        }

        String reasonCd = c.of("CMUG04", plan.reason());
        String rejectedCd = c.of("CMUG05", "반려");
        String doneCd = c.of("CMUG05", "처리완료");
        String receivedCd = c.of("CMUG05", "접수");
        Row last = null;
        for (int i = 0; i < reporters.size(); i++) {
            World.Member reporter = reporters.get(i);
            LocalDateTime at = times.get(i);
            Row dclr = s.store().add("DCLR", r -> {
                r.set("MBR_SN", reporter.sn()).by(reporter.sn());
                r.set(isPost ? "PST_SN" : "CMNT_SN", targetRow.sn());
            });
            dclr.set("DCLR_RSN_CD", reasonCd).set("DCLR_PRCS_STTS_CD", receivedCd).set("REG_DT", at);
            if (plan.detail() != null) {
                dclr.set("DCLR_DTL_CN", plan.detail());
            }
            s.store().add("DCLR_STTS_HSTRY", r -> r.set("DCLR_SN", dclr.sn()).set("MBR_SN", reporter.sn())
                    .set("AFTR_STTS_CD", receivedCd).set("REG_DT", at));
            last = dclr;
            if (processAt == null) {
                continue;
            }
            boolean rejected = outcome == Outcome.REJECT;
            String action = switch (outcome) {
                case BLIND -> "게시물 블라인드";
                case SUSPEND -> "작성자 정지";
                default -> "유지";
            };
            String memo = switch (outcome) {
                case BLIND -> "신고 처리: " + plan.reason() + " 게시물, 블라인드 조치";
                case SUSPEND -> "신고 처리: " + plan.reason() + " 댓글, 작성자 정지 조치";
                case KEEP -> "확인 결과 문제가 없어 유지";
                default -> "신고 사유에 해당하지 않아 반려";
            };
            LocalDateTime doneAt = processAt;
            dclr.set("DCLR_PRCS_STTS_CD", rejected ? rejectedCd : doneCd).set("PRCS_MBR_SN", w.adminSn).set("PRCS_DT", doneAt)
                    .set("MDFCN_DT", doneAt);
            if (!rejected) {
                dclr.set("DCLR_ACTN_CD", c.of("CMUG08", action));
            }
            s.store().add("DCLR_STTS_HSTRY", r -> r.set("DCLR_SN", dclr.sn()).set("MBR_SN", w.adminSn).set("BFR_STTS_CD", receivedCd)
                    .set("AFTR_STTS_CD", rejected ? rejectedCd : doneCd).set("CHG_RSN", memo).set("REG_DT", doneAt));
        }

        String normalCd = c.of("CMUG02", "정상");
        String blindCd = c.of("CMUG02", "블라인드");
        if (outcome == Outcome.BLIND && isPost) {
            LocalDateTime doneAt = processAt;
            targetRow.set("PST_STTS_CD", blindCd).set("MDFCN_DT", doneAt);
            s.store().add("PST_STTS_HSTRY", r -> r.set("PST_SN", targetRow.sn()).set("MBR_SN", w.adminSn).set("BFR_STTS_CD", normalCd)
                    .set("AFTR_STTS_CD", blindCd).set("CHG_RSN", "신고 처리: " + plan.reason() + " 게시물").set("REG_DT", doneAt));
        } else if (outcome == Outcome.AUTO_BLIND && isPost) {
            targetRow.set("PST_STTS_CD", blindCd).set("MDFCN_DT", lastAt);
            s.store().add("PST_STTS_HSTRY", r -> r.set("PST_SN", targetRow.sn()).set("BFR_STTS_CD", normalCd)
                    .set("AFTR_STTS_CD", blindCd).set("CHG_RSN", ReportService.AUTO_BLIND_RSN).set("REG_DT", lastAt));
        } else if (outcome == Outcome.AUTO_DELETE && onComment) {
            Row lastReport = last;
            targetRow.set("DEL_YN", "Y").set("MDFCN_DT", lastAt);
            s.store().add("DCLR_STTS_HSTRY", r -> r.set("DCLR_SN", lastReport.sn()).set("BFR_STTS_CD", receivedCd)
                    .set("AFTR_STTS_CD", receivedCd).set("CHG_RSN", ReportService.AUTO_DELETE_RSN).set("REG_DT", lastAt));
        }
    }

    /** 글(제목이 같은 것)이나 댓글(내용이 match로 시작하는 것)을 작성자로 찾는다. useFallback이면 fallbackTitle 글 */
    private static Row find(Seed s, World.Member author, Target t, boolean useFallback) {
        boolean comment = t.comment() && !useFallback;
        String table = comment ? "CMNT" : "PST";
        String column = comment ? "CMNT_CN" : "PST_TTL";
        String want = useFallback ? t.fallbackTitle() : t.match();
        return s.store().rows(table).stream()
                .filter(r -> ((Number) r.get("MBR_SN")).longValue() == author.sn())
                .filter(r -> comment ? ((String) r.get(column)).startsWith(want) : want.equals(r.get(column)))
                .findFirst().orElse(null);
    }
}
