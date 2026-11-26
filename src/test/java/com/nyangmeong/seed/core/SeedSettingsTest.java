package com.nyangmeong.seed.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.nyangmeong.seed.SeedException;
import com.nyangmeong.seed.SeedOptions.Target;

/**
 * [시드 접속·키 설정]
 * - 시드 계정 비밀번호는 대상과 상관없이 git 제외 application-local.properties에서만 읽는다
 */
class SeedSettingsTest {

    private static final String SHARED = """
            spring.datasource.url=jdbc:log4jdbc:mysql://db.example.com:3306/shared_db?serverTimezone=Asia/Seoul
            spring.datasource.username=user
            spring.datasource.password=pw
            app.crypto.aes-key-base64=aes
            app.crypto.hmac-key-base64=hmac
            app.seed.password=from-shared
            """;

    @TempDir
    Path dir;

    @Test
    void 비밀번호가_없으면_DB에_붙기_전에_멈춘다() throws IOException {
        Files.writeString(dir.resolve("application.properties"), SHARED.replace("app.seed.password=from-shared\n", ""));

        SeedSettings settings = SeedSettings.load(dir, Target.SHARED, null);

        assertThatThrownBy(settings::requiredSeedPassword).isInstanceOf(SeedException.class)
                .hasMessageContaining("app.seed.password").hasMessageContaining("application-local.properties");
    }

    @Test
    void 공용_대상이어도_비밀번호는_application_local에서만_읽는다() throws IOException {
        Files.writeString(dir.resolve("application.properties"), SHARED);
        Files.writeString(dir.resolve("application-local.properties"), "app.seed.password=from-local\n");

        SeedSettings shared = SeedSettings.load(dir, Target.SHARED, null);

        assertThat(shared.requiredSeedPassword()).isEqualTo("from-local");
        assertThat(shared.host()).isEqualTo("db.example.com");
        assertThat(shared.database()).isEqualTo("shared_db");
    }

    @Test
    void 공용_파일의_비밀번호는_쓰지_않는다() throws IOException {
        Files.writeString(dir.resolve("application.properties"), SHARED);

        SeedSettings settings = SeedSettings.load(dir, Target.SHARED, null);

        assertThatThrownBy(settings::requiredSeedPassword).isInstanceOf(SeedException.class);
    }

    @Test
    void 로컬_대상은_DB_이름을_바꿔_접속한다() throws IOException {
        Files.writeString(dir.resolve("application.properties"), SHARED);
        Files.writeString(dir.resolve("application-local.properties"), """
                spring.datasource.url=jdbc:log4jdbc:mysql://localhost:3306/my_local?serverTimezone=Asia/Seoul
                app.seed.password=from-local
                """);

        SeedSettings local = SeedSettings.load(dir, Target.LOCAL, "copy_db");

        assertThat(local.isLocalHost()).isTrue();
        assertThat(local.database()).isEqualTo("copy_db");
        assertThat(local.jdbcUrl()).startsWith("jdbc:mysql://localhost:3306/copy_db?");
    }
}
