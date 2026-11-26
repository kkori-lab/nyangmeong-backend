package com.nyangmeong.seed.core;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.nyangmeong.seed.SeedException;
import com.nyangmeong.seed.SeedOptions.Target;

/**
 * [접속·키 설정]
 * - 서버와 같은 파일을 읽는다. 공용은 application.properties, 로컬은 그 위에 application-local.properties
 * - 시드 계정 비밀번호(app.seed.password)는 대상과 상관없이 git 제외 application-local.properties에서만 읽는다
 */
public record SeedSettings(String jdbcUrl, String username, String password, String host, String database,
        String aesKeyBase64, String hmacKeyBase64, String seedPassword) {

    public static final String SEED_PASSWORD_KEY = "app.seed.password";

    private static final Path RESOURCES = Path.of("src", "main", "resources");
    private static final Pattern MYSQL_URL = Pattern.compile("jdbc:(?:log4jdbc:)?mysql://([^:/?]+)(?::\\d+)?/([^?]+).*");

    public static SeedSettings load(Target target, String databaseOverride) {
        return load(RESOURCES, target, databaseOverride);
    }

    static SeedSettings load(Path resources, Target target, String databaseOverride) {
        Properties shared = read(resources.resolve("application.properties"), true);
        Properties local = read(resources.resolve("application-local.properties"), false);
        Properties props = new Properties();
        props.putAll(shared);
        if (target == Target.LOCAL) {
            props.putAll(local);
        }
        String url = required(props, "spring.datasource.url");
        Matcher m = MYSQL_URL.matcher(url);
        if (!m.matches()) {
            throw new SeedException("spring.datasource.url이 jdbc:mysql://주소/DB 형식이 아닙니다");
        }
        // 시드는 MySQL 드라이버로 직접 연결하므로 서버용 log4jdbc 접두어는 뗀다. 대량 INSERT는 여러 행 한 문장으로 묶는다
        String plainUrl = url.replaceFirst("^jdbc:log4jdbc:", "jdbc:");
        String database = m.group(2);
        if (databaseOverride != null) {
            plainUrl = plainUrl.replaceFirst("/" + Pattern.quote(database) + "(?=\\?|$)", "/" + databaseOverride);
            database = databaseOverride;
        }
        String jdbcUrl = plainUrl + (plainUrl.contains("?") ? "&" : "?") + "rewriteBatchedStatements=true";
        return new SeedSettings(jdbcUrl, required(props, "spring.datasource.username"),
                props.getProperty("spring.datasource.password", ""), m.group(1), database,
                required(props, "app.crypto.aes-key-base64"), required(props, "app.crypto.hmac-key-base64"),
                local.getProperty(SEED_PASSWORD_KEY));
    }

    /**
     * 시드 계정 비밀번호. 공개 저장소라 코드·README에 두지 않고 application-local.properties에만 둔다
     */
    public String requiredSeedPassword() {
        if (seedPassword == null || seedPassword.isBlank()) {
            throw new SeedException(SEED_PASSWORD_KEY
                    + " 값이 없습니다. src/main/resources/application-local.properties(git 제외)에 넣어 주세요");
        }
        return seedPassword.trim();
    }

    public boolean isLocalHost() {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
    }

    public String describeDatabase() {
        return host + "/" + database;
    }

    private static Properties read(Path path, boolean requiredFile) {
        Properties props = new Properties();
        if (!Files.isRegularFile(path)) {
            if (requiredFile) {
                throw new SeedException(path + " 파일이 없습니다. 디스코드의 최신 파일을 받아 주세요");
            }
            return props;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            props.load(reader);
            return props;
        } catch (IOException e) {
            throw new SeedException(path + "을 읽지 못했습니다: " + e.getMessage());
        }
    }

    private static String required(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new SeedException(key + " 값이 없습니다. application.properties를 확인해 주세요");
        }
        return value.trim();
    }
}
