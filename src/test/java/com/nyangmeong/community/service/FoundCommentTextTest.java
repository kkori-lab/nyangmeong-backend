package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * [검사 - 찾았어요 자동 댓글 문구의 [펫이름]·조사 맞춤]
 */
class FoundCommentTextTest {

    private static final String DEFAULT = "🎉 [펫이름]가 무사히 가족 품으로 돌아왔어요!";

    @Test
    void 받침이_없으면_가_있으면_이() {
        assertThat(FoundCommentText.render(DEFAULT, "초코")).isEqualTo("🎉 초코가 무사히 가족 품으로 돌아왔어요!");
        assertThat(FoundCommentText.render(DEFAULT, "콩이")).isEqualTo("🎉 콩이가 무사히 가족 품으로 돌아왔어요!");
        assertThat(FoundCommentText.render(DEFAULT, "밤")).isEqualTo("🎉 밤이 무사히 가족 품으로 돌아왔어요!");
    }

    @Test
    void 관리자가_어느_형태로_써도_받침에_맞춘다() {
        assertThat(FoundCommentText.render("[펫이름]이 돌아왔어요", "보리")).isEqualTo("보리가 돌아왔어요");
        assertThat(FoundCommentText.render("[펫이름]는 집에 있어요", "별")).isEqualTo("별은 집에 있어요");
        assertThat(FoundCommentText.render("[펫이름]을 찾았어요", "콩이")).isEqualTo("콩이를 찾았어요");
        assertThat(FoundCommentText.render("[펫이름]와 함께", "밤")).isEqualTo("밤과 함께");
        assertThat(FoundCommentText.render("[펫이름]과 함께", "보리")).isEqualTo("보리와 함께");
    }

    @Test
    void 한글로_끝나지_않는_이름은_두_형태를_다_보인다() {
        assertThat(FoundCommentText.render(DEFAULT, "Max")).isEqualTo("🎉 Max(이)가 무사히 가족 품으로 돌아왔어요!");
        assertThat(FoundCommentText.render("[펫이름]를 찾았어요", "쿠키2")).isEqualTo("쿠키2(을)를 찾았어요");
    }

    @Test
    void 토큰은_대괄호째_모두_바꾸고_조사가_없으면_이름만_넣는다() {
        assertThat(FoundCommentText.render("[펫이름]! [펫이름]가 왔어요", "밤")).isEqualTo("밤! 밤이 왔어요");
        assertThat(FoundCommentText.render("반가워요 [펫이름]", "초코")).isEqualTo("반가워요 초코");
        assertThat(FoundCommentText.render("[펫이름]의 귀가", "밤")).isEqualTo("밤의 귀가");
    }

    @Test
    void 조사_뒤에_한글이_이어지면_조사로_보지_않는다() {
        assertThat(FoundCommentText.render("우리 [펫이름]이에요", "초코")).isEqualTo("우리 초코이에요");
        assertThat(FoundCommentText.render("[펫이름]가족 품으로", "밤")).isEqualTo("밤가족 품으로");
    }

    @Test
    void 토큰이_없거나_값이_비면_그대로_둔다() {
        assertThat(FoundCommentText.render("돌아왔어요!", "초코")).isEqualTo("돌아왔어요!");
        assertThat(FoundCommentText.render("[펫이름]가 왔어요", null)).isEqualTo("가 왔어요");
        assertThat(FoundCommentText.render(null, "초코")).isEmpty();
        assertThat(FoundCommentText.render("[펫이름] $1 \\", "초코")).isEqualTo("초코 $1 \\");
    }
}
