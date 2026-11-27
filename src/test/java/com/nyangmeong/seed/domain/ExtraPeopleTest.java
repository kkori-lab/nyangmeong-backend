package com.nyangmeong.seed.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.nyangmeong.seed.core.World.Activity;
import com.nyangmeong.seed.domain.CommunitySeeder.C;
import com.nyangmeong.seed.domain.CommunitySeeder.P;

/**
 * [새 엑스트라와 글·댓글 데이터]
 * - DB 없이 데이터 자체가 맞는지 본다 : 사람 같은 값, 활동량·가입일·정지·휴면·탈퇴와 글·댓글 시각, 신고 대상
 */
class ExtraPeopleTest {

    /** 티 나는 값 : 회원23·시드용·테스트 같은 말 */
    private static final Pattern FAKE = Pattern.compile("회원\\d+|시드|테스트|test|더미|샘플|dummy", Pattern.CASE_INSENSITIVE);

    private static final Map<String, People.PersonPlan> NEW = new HashMap<>();

    static {
        People.everyExtra().stream().filter(p -> p.keptSn() == null).forEach(p -> NEW.put(p.key(), p));
    }

    private static int count(Activity a) {
        return (int) ExtraPeople.PLANS.stream().filter(p -> p.activity() == a).count();
    }

    @Test
    void 새_엑스트라는_50명_이상이고_활동량이_고르게_섞여_있다() {
        assertThat(ExtraPeople.PLANS).hasSizeGreaterThanOrEqualTo(50);
        assertThat(count(Activity.HEAVY)).isGreaterThanOrEqualTo(5);
        assertThat(count(Activity.NORMAL)).isGreaterThanOrEqualTo(12);
        assertThat(count(Activity.LIGHT)).isGreaterThanOrEqualTo(12);
        assertThat(count(Activity.LURKER)).isGreaterThanOrEqualTo(8);
        assertThat(count(Activity.DORMANT)).isGreaterThanOrEqualTo(6);
    }

    @Test
    void 키_닉네임_메일이_겹치지_않는다() {
        List<People.PersonPlan> all = new ArrayList<>(People.everyExtra());
        all.add(People.PERSONA);
        assertThat(all.stream().map(People.PersonPlan::key)).doesNotHaveDuplicates();
        assertThat(all.stream().map(People.PersonPlan::nickname).filter(n -> n != null)).doesNotHaveDuplicates();
        assertThat(all.stream().map(People.PersonPlan::emailLocal).filter(n -> n != null)).doesNotHaveDuplicates();
        List<String> petKeys = new ArrayList<>();
        all.forEach(p -> p.pets().forEach(pet -> petKeys.add(pet.key())));
        assertThat(petKeys).doesNotHaveDuplicates();
    }

    @Test
    void 티_나는_값이_없다() {
        for (People.PersonPlan p : ExtraPeople.PLANS) {
            assertThat(FAKE.matcher(p.nickname()).find()).as(p.nickname()).isFalse();
            assertThat(p.pets().size()).as(p.key()).isLessThanOrEqualTo(3);
            for (People.PetPlan pet : p.pets()) {
                assertThat(FAKE.matcher(pet.name() + pet.trait()).find()).as(pet.key()).isFalse();
            }
        }
        for (P post : ExtraPosts.POSTS) {
            assertThat(FAKE.matcher(post.title() + post.content()).find()).as(post.title()).isFalse();
            for (C c : post.comments()) {
                assertThat(FAKE.matcher(c.text()).find()).as(c.text()).isFalse();
            }
        }
    }

    @Test
    void 가입일은_최근_1년이고_90일_안에도_여럿_있다() {
        assertThat(ExtraPeople.PLANS).allMatch(p -> p.joinedDays() >= 1 && p.joinedDays() <= 365);
        assertThat(ExtraPeople.PLANS.stream().filter(p -> p.joinedDays() <= 90).count()).isGreaterThanOrEqualTo(15);
    }

    @Test
    void 반려동물_등록일이_가입일보다_앞서지_않는다() {
        for (People.PersonPlan p : ExtraPeople.PLANS) {
            for (People.PetPlan pet : p.pets()) {
                assertThat(pet.registeredDays()).as(p.key() + "/" + pet.key()).isLessThanOrEqualTo(p.joinedDays());
            }
        }
    }

    @Test
    void 휴면_정지_탈퇴_정보가_맞다() {
        for (People.PersonPlan p : ExtraPeople.PLANS) {
            ExtraPeople.Meta meta = ExtraPeople.META.get(p.key());
            if (p.activity() == Activity.DORMANT) {
                assertThat(meta).as(p.key()).isNotNull();
                assertThat(meta.daysAgo()).isBetween(60, 120);
            }
            if (meta != null) {
                assertThat(meta.daysAgo()).as(p.key()).isLessThan(p.joinedDays());
            }
            if (meta != null && meta.status() == ExtraPeople.Status.SUSPENDED) {
                // 7일 정지가 아직 풀리지 않아야 한다
                assertThat(meta.daysAgo()).isLessThanOrEqualTo(6);
            }
        }
        assertThat(ExtraPeople.META.values().stream().filter(m -> m.status() == ExtraPeople.Status.WITHDRAWN).count()).isEqualTo(4);
        assertThat(ExtraPeople.META.values().stream().filter(m -> m.status() == ExtraPeople.Status.BANNED).count()).isEqualTo(1);
        assertThat(ExtraPeople.META.values().stream().filter(m -> m.status() == ExtraPeople.Status.SUSPENDED).count()).isEqualTo(1);
    }

    /** 활동이 끝난 날(며칠 전). 없으면 -1 */
    private static int endedDaysAgo(String key) {
        ExtraPeople.Meta meta = ExtraPeople.META.get(key);
        return meta == null ? -1 : meta.daysAgo();
    }

    @Test
    void 글_댓글은_가입한_뒤_활동이_끝나기_전에만_있다() {
        for (P post : ExtraPosts.POSTS) {
            People.PersonPlan author = NEW.get(post.key());
            assertThat(author).as("글쓴이 " + post.key()).isNotNull();
            assertThat(author.activity()).as(post.title()).isNotIn(Activity.LURKER, Activity.NONE);
            assertThat(post.daysAgo()).as(post.title()).isLessThan(author.joinedDays()).isGreaterThanOrEqualTo(1);
            assertThat(post.daysAgo()).as(post.title()).isGreaterThan(endedDaysAgo(post.key()));
            for (int i = 0; i < post.comments().size(); i++) {
                C c = post.comments().get(i);
                People.PersonPlan writer = NEW.get(c.key());
                assertThat(writer).as("댓글 " + c.key()).isNotNull();
                int day = post.daysAgo() - c.minutes() / 1440;
                assertThat(day).as(post.title() + "/" + c.key()).isGreaterThanOrEqualTo(0).isLessThan(writer.joinedDays())
                        .isGreaterThan(endedDaysAgo(c.key()));
                if (c.reply() >= 0) {
                    assertThat(c.reply()).isLessThan(i);
                }
            }
        }
    }

    @Test
    void 글_양은_활동량에서_나온다() {
        Map<String, Integer> counts = new HashMap<>();
        ExtraPosts.POSTS.forEach(p -> counts.merge(p.key(), 1, Integer::sum));
        assertThat(ExtraPosts.POSTS.size()).isGreaterThanOrEqualTo(60);
        assertThat(ExtraPosts.POSTS.stream().mapToInt(p -> p.comments().size()).sum()).isGreaterThanOrEqualTo(100);
        for (People.PersonPlan p : ExtraPeople.PLANS) {
            int n = counts.getOrDefault(p.key(), 0);
            switch (p.activity()) {
            case HEAVY -> assertThat(n).as(p.key()).isBetween(3, 6);
            case NORMAL -> assertThat(n).as(p.key()).isBetween(0, 3);
            case LURKER -> assertThat(n).as(p.key()).isZero();
            default -> assertThat(n).as(p.key()).isLessThanOrEqualTo(3);
            }
        }
    }

    @Test
    void 신고_대상_글과_댓글이_글_데이터에_있다() {
        Set<String> titles = new HashSet<>();
        ExtraPosts.POSTS.forEach(p -> titles.add(p.key() + "|" + p.title()));
        for (ReportSeeder.Plan plan : ReportSeeder.PLANS) {
            ReportSeeder.Target t = plan.target();
            if (!t.comment()) {
                assertThat(titles).as(t.match()).contains(t.authorKey() + "|" + t.match());
                continue;
            }
            boolean found = ExtraPosts.POSTS.stream().flatMap(p -> p.comments().stream())
                    .anyMatch(c -> c.key().equals(t.authorKey()) && c.text().startsWith(t.match()));
            assertThat(found).as(t.match()).isTrue();
        }
        // 자동 처리 대상은 글 2·댓글 1
        assertThat(ReportSeeder.PLANS.stream().filter(p -> p.outcome() == ReportSeeder.Outcome.AUTO_BLIND).count()).isEqualTo(2);
        assertThat(ReportSeeder.PLANS.stream().filter(p -> p.outcome() == ReportSeeder.Outcome.AUTO_DELETE).count()).isEqualTo(1);
    }
}
