package com.nyangmeong.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * [검사 - 설정 예시 파일·운영 설정]
 * - Spring Boot 는 .properties 를 ISO-8859-1 로 읽어 한글 값이 깨진다(예: 뉴스 검색어가 깨져 수집이 오류 없이 0건)
 * - 예시를 복사해 쓰므로 주석·빈 줄이 아닌 줄에 ASCII 밖 문자를 두지 않는다
 */
class PropertiesExampleTest {

    private static final Path EXAMPLE = Path.of("src/main/resources/application.properties.example");
    private static final Path PROD = Path.of("src/main/resources/application-prod.properties");

    @Test
    void 예시_파일의_설정_줄에는_ASCII_밖_문자가_없다() throws IOException {
        List<String> nonAscii = Files.readAllLines(EXAMPLE, StandardCharsets.UTF_8).stream()
                .map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#") && !line.startsWith("!"))
                .filter(line -> line.chars().anyMatch(c -> c > 0x7F))
                .toList();

        assertThat(nonAscii).as("한글 등 ASCII 밖 값은 주석으로 두거나 \\uXXXX 로 적는다").isEmpty();
    }

    @Test
    void 운영_설정은_좌표_행정구역_변환에_카카오를_쓴다() throws IOException {
        Properties prod = new Properties();
        try (var reader = Files.newBufferedReader(PROD, StandardCharsets.UTF_8)) {
            prod.load(reader);
        }

        assertThat(prod.getProperty("app.region.client"))
                .as("없으면 기본값 fake 라 운영의 모든 실종·펫돌봄 글이 전주시로 저장된다").isEqualTo("kakao");
    }
}
