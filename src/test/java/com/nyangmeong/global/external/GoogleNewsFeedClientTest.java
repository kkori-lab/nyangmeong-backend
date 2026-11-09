package com.nyangmeong.global.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

import tools.jackson.databind.ObjectMapper;

/**
 * [검사 - Google 뉴스 RSS 파싱]
 * - 네트워크 없이 예제 응답으로 파싱만 확인
 */
class GoogleNewsFeedClientTest {

    private static final String FEED = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <rss version="2.0"><channel><title>"반려동물" - Google 뉴스</title><link>https://news.google.com/</link>
            <item><title>[팩트추적 예고] 반려동물 유통 시장 충격적인 민낯 - YTN</title>
            <link>https://news.google.com/rss/articles/CBMiAAA?oc=5</link><guid isPermaLink="false">CBMiAAA</guid>
            <pubDate>Mon, 02 Nov 2026 01:01:33 GMT</pubDate>
            <description>&lt;a href="x"&gt;제목&lt;/a&gt;</description><source url="https://www.ytn.co.kr">YTN</source></item>
            <item><title>언론사 꼬리가 없는 제목</title>
            <link>https://news.google.com/rss/articles/CBMiBBB?oc=5</link>
            <pubDate>Sun, 01 Nov 2026 14:59:59 GMT</pubDate></item>
            <item><title>링크가 없는 기사</title><pubDate>Sun, 01 Nov 2026 14:59:59 GMT</pubDate></item>
            <item><title>날짜가 깨진 기사</title><link>https://news.google.com/rss/articles/CBMiCCC</link>
            <pubDate>어제</pubDate></item>
            </channel></rss>
            """;

    private final GoogleNewsFeedClient client = new GoogleNewsFeedClient(new NewsFeedProperties(),
            new ObjectMapper());

    @Test
    void 기본_검색어는_반려_낱말만이고_강아지_고양이_단독_검색어는_없다() {
        String query = new NewsFeedProperties().getQuery();

        assertThat(query).isEqualTo("반려동물 OR 반려견 OR 반려묘 OR \"반려 동물\"");
        assertThat(query.split(" OR ")).doesNotContain("강아지", "고양이");
    }

    @Test
    void RSS는_제목_언론사_링크_발행일을_읽고_발행일은_한국_시간이다() {
        List<NewsFeedItem> items = GoogleNewsFeedClient.parseFeed(FEED);

        assertThat(items).hasSize(2);
        NewsFeedItem first = items.get(0);
        assertThat(first.title()).isEqualTo("[팩트추적 예고] 반려동물 유통 시장 충격적인 민낯");
        assertThat(first.pressNm()).isEqualTo("YTN");
        assertThat(first.link()).isEqualTo("https://news.google.com/rss/articles/CBMiAAA?oc=5");
        assertThat(first.pblcnDt()).isEqualTo(LocalDateTime.of(2026, 11, 2, 10, 1, 33));
    }

    @Test
    void 언론사가_없으면_제목을_그대로_두고_언론사는_null() {
        NewsFeedItem second = GoogleNewsFeedClient.parseFeed(FEED).get(1);

        assertThat(second.title()).isEqualTo("언론사 꼬리가 없는 제목");
        assertThat(second.pressNm()).isNull();
        assertThat(second.pblcnDt()).isEqualTo(LocalDateTime.of(2026, 11, 1, 23, 59, 59));
    }

    @Test
    void 링크나_날짜가_없는_항목은_건너뛴다() {
        List<String> titles = GoogleNewsFeedClient.parseFeed(FEED).stream().map(NewsFeedItem::title).toList();

        assertThat(titles).doesNotContain("링크가 없는 기사", "날짜가 깨진 기사");
    }

    @Test
    void DOCTYPE이_든_XML은_거절한다() {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE rss [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><rss>&x;</rss>";

        assertThatThrownBy(() -> GoogleNewsFeedClient.parseFeed(xxe))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NEWS_FEED_FETCH_FAILED));
    }

    @Test
    void 원문_페이지에서_og_description과_og_image를_읽는다() {
        String html = """
                <html><head>
                <meta property="og:image:width" content="1280" />
                <meta property="og:image" content="/img/a.jpg?x=1&amp;y=2" />
                <meta property="og:description" content="입양 절차를 &quot;간소화&quot;하고
                   지원금을 늘린다" />
                </head></html>
                """;

        NewsPreview preview = GoogleNewsFeedClient.parseMeta(html, URI.create("https://news.example.com/a/1"));

        assertThat(preview.description()).isEqualTo("입양 절차를 \"간소화\"하고 지원금을 늘린다");
        assertThat(preview.imageUrl()).isEqualTo("https://news.example.com/img/a.jpg?x=1&y=2");
    }

    @Test
    void 속성_순서가_달라도_작은따옴표여도_읽는다() {
        String html = "<meta content='요약 문장' name='og:description'><meta content=\"http://img.example.com/b.png\" property=\"og:image\">";

        NewsPreview preview = GoogleNewsFeedClient.parseMeta(html, URI.create("https://news.example.com/"));

        assertThat(preview.description()).isEqualTo("요약 문장");
        assertThat(preview.imageUrl()).isEqualTo("http://img.example.com/b.png");
    }

    @Test
    void 메타가_없거나_http가_아닌_이미지는_null() {
        NewsPreview none = GoogleNewsFeedClient.parseMeta("<html></html>", URI.create("https://a.example.com/"));
        NewsPreview data = GoogleNewsFeedClient.parseMeta(
                "<meta property=\"og:image\" content=\"data:image/png;base64,AAAA\">", URI.create("https://a.example.com/"));
        NewsPreview tooLong = GoogleNewsFeedClient.parseMeta(
                "<meta property=\"og:image\" content=\"https://a.example.com/" + "x".repeat(500) + "\">",
                URI.create("https://a.example.com/"));

        assertThat(none.description()).isNull();
        assertThat(none.imageUrl()).isNull();
        assertThat(data.imageUrl()).isNull();
        assertThat(tooLong.imageUrl()).isNull();
    }

    @Test
    void 기사_페이지에서_원문_주소_풀기_값을_읽는다() {
        String html = "<c-wiz data-n-a-sg=\"AdwtSYOU3RKk\" data-n-a-ts=\"1793582119\" data-n-a-id=\"CBMid0FVX3lx\">";

        GoogleNewsFeedClient.DecodeParams params = GoogleNewsFeedClient.parseDecodeParams(html);

        assertThat(params).isEqualTo(new GoogleNewsFeedClient.DecodeParams("CBMid0FVX3lx", "1793582119", "AdwtSYOU3RKk"));
        assertThat(GoogleNewsFeedClient.parseDecodeParams("<html>값 없음</html>")).isNull();
    }

    @Test
    void batchexecute_응답에서_원문_주소를_꺼낸다() {
        String body = ")]}'\n\n[[\"wrb.fr\",\"Fbv4je\",\"[\\\"garturlres\\\",\\\"https://www.ytn.co.kr/replay/view.php?idx\\\\u003d274\\\\u0026key\\\\u003d1\\\",1]\","
                + "null,null,null,\"generic\"],[\"di\",13],[\"af.httprm\",12,\"1\",86]]";

        assertThat(client.parseDecodedUrl(body)).isEqualTo("https://www.ytn.co.kr/replay/view.php?idx=274&key=1");
    }

    @Test
    void 예상과_다른_응답은_null() {
        assertThat(client.parseDecodedUrl(null)).isNull();
        assertThat(client.parseDecodedUrl("<html>차단</html>")).isNull();
        assertThat(client.parseDecodedUrl("[[\"wrb.fr\",\"Fbv4je\",null,null]]")).isNull();
    }

    @Test
    void 응답_인코딩은_헤더_다음_meta_다음_UTF8_순서로_정한다() {
        byte[] euckr = "<meta charset=\"euc-kr\">".getBytes(StandardCharsets.ISO_8859_1);

        assertThat(GoogleNewsFeedClient.charsetOf("text/html; charset=EUC-KR", new byte[0]))
                .isEqualTo(Charset.forName("EUC-KR"));
        assertThat(GoogleNewsFeedClient.charsetOf("text/html", euckr)).isEqualTo(Charset.forName("EUC-KR"));
        assertThat(GoogleNewsFeedClient.charsetOf("text/html", new byte[0])).isEqualTo(StandardCharsets.UTF_8);
        assertThat(GoogleNewsFeedClient.charsetOf("text/html; charset=nope", new byte[0]))
                .isEqualTo(StandardCharsets.UTF_8);
    }
}
