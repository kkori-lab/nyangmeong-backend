package com.nyangmeong.global.external;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * [Google 뉴스 RSS 클라이언트]
 * - 검색 RSS 로 제목·언론사·발행일·링크, 링크의 원문 주소를 풀어 원문 페이지의 og:description·og:image
 * - 원문 주소 풀기는 Google 뉴스 페이지의 비공식 호출이라 바뀌면 실패 : 미리보기는 빈 값으로 돌려 수집은 계속
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleNewsFeedClient implements NewsFeedClient {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final String FEED_HOST = "news.google.com";
    private static final String BATCH_URL = "https://news.google.com/_/DotsSplashUi/data/batchexecute";
    private static final int FEED_MAX_BYTES = 4 * 1024 * 1024;
    private static final int GOOGLE_PAGE_MAX_BYTES = 2 * 1024 * 1024;
    private static final int ARTICLE_PAGE_MAX_BYTES = 512 * 1024;
    private static final int DESCRIPTION_MAX_LENGTH = 4000;
    private static final int IMAGE_URL_MAX_LENGTH = 500;
    private static final Pattern DECODE_ID = Pattern.compile("data-n-a-id=\"([A-Za-z0-9_-]+)\"");
    private static final Pattern DECODE_TS = Pattern.compile("data-n-a-ts=\"(\\d+)\"");
    private static final Pattern DECODE_SG = Pattern.compile("data-n-a-sg=\"([A-Za-z0-9_-]+)\"");
    private static final Pattern META_TAG = Pattern.compile("<meta\\s[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern META_KEY = Pattern.compile(
            "\\b(?:property|name)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')", Pattern.CASE_INSENSITIVE);
    private static final Pattern META_CONTENT = Pattern.compile(
            "\\bcontent\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHARSET = Pattern.compile("charset\\s*=\\s*[\"']?([\\w-]+)",
            Pattern.CASE_INSENSITIVE);

    private final NewsFeedProperties properties;
    private final ObjectMapper objectMapper;

    private HttpClient httpClient;

    @Override
    public List<NewsFeedItem> fetchFeed(LocalDate after, LocalDate before) {
        String query = properties.getQuery() + " after:" + after + " before:" + before;
        URI uri = UriComponentsBuilder.fromUriString(properties.getRssUrl())
                .queryParam("q", "{q}")
                .queryParam("hl", "ko")
                .queryParam("gl", "KR")
                .queryParam("ceid", "KR:ko")
                .encode()
                .buildAndExpand(query)
                .toUri();
        try {
            Page page = get(uri, FEED_MAX_BYTES);
            if (page.status() != 200) {
                log.warn("[뉴스 수집] RSS 응답 status={}", page.status());
                throw new CustomException(ErrorCode.NEWS_FEED_FETCH_FAILED);
            }
            return parseFeed(page.text());
        } catch (IOException e) {
            log.warn("[뉴스 수집] RSS 요청 실패 {}", e.toString());
            throw new CustomException(ErrorCode.NEWS_FEED_FETCH_FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.NEWS_FEED_FETCH_FAILED);
        }
    }

    @Override
    public NewsPreview fetchPreview(String feedLink) {
        try {
            URI articleUri = resolveArticleUri(feedLink);
            if (articleUri == null || !isPublicHttpUrl(articleUri)) {
                return NewsPreview.empty();
            }
            Page page = get(articleUri, ARTICLE_PAGE_MAX_BYTES);
            if (page.status() != 200) {
                return NewsPreview.empty();
            }
            return parseMeta(page.text(), page.uri());
        } catch (IOException | RuntimeException e) {
            log.warn("[뉴스 수집] 미리보기 실패 {}", e.toString());
            return NewsPreview.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return NewsPreview.empty();
        }
    }

    /**
     * 피드 링크 -> 원문 주소 : 기사 페이지의 서명값으로 batchexecute 호출
     */
    private URI resolveArticleUri(String feedLink) throws IOException, InterruptedException {
        URI feedUri = URI.create(feedLink);
        if (!FEED_HOST.equals(feedUri.getHost())) {
            return feedUri;
        }
        Page google = get(feedUri, GOOGLE_PAGE_MAX_BYTES);
        DecodeParams params = parseDecodeParams(google.text());
        if (params == null) {
            return null;
        }
        String body = "f.req=" + URLEncoder.encode(batchRequest(params), StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(BATCH_URL))
                .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .header("User-Agent", USER_AGENT)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = client().send(request, HttpResponse.BodyHandlers.ofString());
        String url = parseDecodedUrl(response.body());
        return url == null ? null : URI.create(url);
    }

    private String batchRequest(DecodeParams params) {
        String inner = "[\"garturlreq\",[[\"X\",\"X\",[\"X\",\"X\"],null,null,1,1,\"US:en\",null,1,null,null,null,null,null,0,1],"
                + "\"X\",\"X\",1,[1,1,1],1,1,null,0,0,null,0],\"" + params.id() + "\"," + params.ts() + ",\""
                + params.sg() + "\"]";
        return objectMapper.writeValueAsString(List.of(List.of(Arrays.asList("Fbv4je", inner, null, "generic"))));
    }

    private Page get(URI uri, int maxBytes) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "ko-KR,ko;q=0.9")
                .GET()
                .build();
        HttpResponse<InputStream> response = client().send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream in = response.body()) {
            byte[] bytes = in.readNBytes(maxBytes);
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            return new Page(response.statusCode(), response.uri(), new String(bytes, charsetOf(contentType, bytes)));
        }
    }

    private synchronized HttpClient client() {
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        }
        return httpClient;
    }

    /**
     * 응답 인코딩 : 헤더, 없으면 본문 앞부분의 meta charset, 없으면 UTF-8
     */
    static Charset charsetOf(String contentType, byte[] bytes) {
        Matcher header = CHARSET.matcher(contentType);
        if (header.find()) {
            return toCharset(header.group(1));
        }
        String head = new String(bytes, 0, Math.min(bytes.length, 2048), StandardCharsets.ISO_8859_1);
        Matcher meta = CHARSET.matcher(head);
        return meta.find() ? toCharset(meta.group(1)) : StandardCharsets.UTF_8;
    }

    private static Charset toCharset(String name) {
        try {
            return Charset.forName(name);
        } catch (IllegalArgumentException e) {
            return StandardCharsets.UTF_8;
        }
    }

    /**
     * RSS 파싱 : 제목 끝의 " - 언론사"는 뗌
     */
    static List<NewsFeedItem> parseFeed(String xml) {
        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            log.warn("[뉴스 수집] RSS 파싱 실패 {}", e.toString());
            throw new CustomException(ErrorCode.NEWS_FEED_FETCH_FAILED);
        }
        List<NewsFeedItem> items = new ArrayList<>();
        NodeList nodes = document.getElementsByTagName("item");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element item = (Element) nodes.item(i);
            String title = childText(item, "title");
            String link = childText(item, "link");
            String pressNm = childText(item, "source");
            LocalDateTime pblcnDt = parsePubDate(childText(item, "pubDate"));
            if (title == null || link == null || pblcnDt == null) {
                continue;
            }
            items.add(new NewsFeedItem(stripPress(title, pressNm), link, pressNm, pblcnDt));
        }
        return items;
    }

    private static String childText(Element parent, String tag) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && tag.equals(child.getNodeName())) {
                String text = child.getTextContent().trim();
                return text.isEmpty() ? null : text;
            }
        }
        return null;
    }

    private static LocalDateTime parsePubDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).withZoneSameInstant(ZONE)
                    .toLocalDateTime();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String stripPress(String title, String pressNm) {
        if (pressNm != null && title.endsWith(" - " + pressNm)) {
            return title.substring(0, title.length() - pressNm.length() - 3).trim();
        }
        return title;
    }

    /**
     * 기사 페이지에서 원문 주소 풀기에 쓰는 값
     */
    static DecodeParams parseDecodeParams(String html) {
        Matcher id = DECODE_ID.matcher(html);
        Matcher ts = DECODE_TS.matcher(html);
        Matcher sg = DECODE_SG.matcher(html);
        if (id.find() && ts.find() && sg.find()) {
            return new DecodeParams(id.group(1), ts.group(1), sg.group(1));
        }
        return null;
    }

    /**
     * batchexecute 응답에서 원문 주소 : 응답은 ")]}'" 뒤에 JSON, 그 안의 문자열이 다시 JSON
     */
    String parseDecodedUrl(String body) {
        int start = body == null ? -1 : body.indexOf("[[\"wrb.fr\"");
        if (start < 0) {
            return null;
        }
        try {
            JsonNode outer = objectMapper.readTree(body.substring(start));
            JsonNode inner = objectMapper.readTree(outer.get(0).get(2).asString());
            String url = inner.get(1).asString();
            return url != null && url.startsWith("http") ? url : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 원문 페이지의 og:description·og:image
     */
    static NewsPreview parseMeta(String html, URI base) {
        String description = null;
        String image = null;
        Matcher tags = META_TAG.matcher(html);
        while (tags.find() && (description == null || image == null)) {
            String tag = tags.group();
            String key = group(META_KEY.matcher(tag));
            String content = group(META_CONTENT.matcher(tag));
            if (key == null || content == null) {
                continue;
            }
            if (description == null && "og:description".equalsIgnoreCase(key)) {
                description = cleanText(content);
            } else if (image == null && "og:image".equalsIgnoreCase(key)) {
                image = absoluteImage(content, base);
            }
        }
        return new NewsPreview(description, image);
    }

    private static String group(Matcher matcher) {
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
    }

    private static String cleanText(String raw) {
        String text = HtmlUtils.htmlUnescape(raw).replaceAll("\\s+", " ").trim();
        if (text.isEmpty()) {
            return null;
        }
        return text.length() > DESCRIPTION_MAX_LENGTH ? text.substring(0, DESCRIPTION_MAX_LENGTH) : text;
    }

    private static String absoluteImage(String raw, URI base) {
        try {
            URI resolved = base.resolve(HtmlUtils.htmlUnescape(raw).trim());
            String scheme = resolved.getScheme();
            String url = resolved.toString();
            boolean http = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            return http && url.length() <= IMAGE_URL_MAX_LENGTH ? url : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 내부망 주소로의 요청 방지
     */
    private static boolean isPublicHttpUrl(URI uri) throws IOException {
        String scheme = uri.getScheme();
        if (uri.getHost() == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            return false;
        }
        InetAddress address = InetAddress.getByName(uri.getHost());
        return !(address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()
                || address.isAnyLocalAddress());
    }

    private record Page(int status, URI uri, String text) {
    }

    record DecodeParams(String id, String ts, String sg) {
    }
}
