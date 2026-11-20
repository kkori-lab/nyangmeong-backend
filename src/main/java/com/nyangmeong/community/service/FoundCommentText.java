package com.nyangmeong.community.service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * [찾았어요 자동 댓글 문구 만들기]
 * - 운영 설정 문구(SYS_STNG.DSCV_AUTO_CMNT_CN)의 [펫이름](대괄호 포함)을 펫 이름으로 모두 바꾼다
 * - 바로 뒤 조사(이/가·은/는·을/를·과/와, 어느 쪽으로 써도)를 이름 마지막 글자 받침에 맞춘다
 * - 마지막 글자가 한글 음절이 아니면(영문·숫자 등) 받침을 알 수 없어 「(이)가」처럼 둘 다 보인다
 * - 찾았어요 전환 댓글과 실종 글 상세의 미리보기가 같은 결과를 쓴다 (사람 요청 2026-11-20)
 */
final class FoundCommentText {

    static final String PET_NAME_TOKEN = "[펫이름]";

    /**
     * 토큰 뒤 조사 : 조사 뒤에 한글이 이어지면(「[펫이름]이에요」·「[펫이름]가족」) 조사가 아니라 보고 그대로 둔다
     */
    private static final Pattern TOKEN_WITH_PARTICLE = Pattern
            .compile(Pattern.quote(PET_NAME_TOKEN) + "(?:(이|가|은|는|을|를|과|와)(?![가-힣]))?");

    private static final Map<String, String[]> PARTICLES = Map.of(
            "이", new String[] { "이", "가" }, "가", new String[] { "이", "가" },
            "은", new String[] { "은", "는" }, "는", new String[] { "은", "는" },
            "을", new String[] { "을", "를" }, "를", new String[] { "을", "를" },
            "과", new String[] { "과", "와" }, "와", new String[] { "과", "와" });

    private static final char HANGUL_FIRST = '가';
    private static final char HANGUL_LAST = '힣';

    private FoundCommentText() {
    }

    /**
     * 설정 문구에 펫 이름을 넣은 댓글 문구
     *
     * @param template 운영 설정 문구 (null 이면 빈 문자열)
     * @param petNm    펫 이름 (null 이면 빈 문자열)
     */
    static String render(String template, String petNm) {
        if (template == null) {
            return "";
        }
        String name = petNm == null ? "" : petNm.strip();
        Matcher matcher = TOKEN_WITH_PARTICLE.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String particle = matcher.group(1);
            String replacement = particle == null ? name : name + particleFor(name, particle);
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String particleFor(String name, String written) {
        String[] pair = PARTICLES.get(written);
        if (name.isEmpty()) {
            return written;
        }
        char last = name.charAt(name.length() - 1);
        if (last < HANGUL_FIRST || last > HANGUL_LAST) {
            return "(" + pair[0] + ")" + pair[1];
        }
        boolean hasFinal = (last - HANGUL_FIRST) % 28 != 0;
        return hasFinal ? pair[0] : pair[1];
    }
}
