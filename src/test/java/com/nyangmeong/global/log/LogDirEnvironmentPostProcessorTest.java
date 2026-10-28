package com.nyangmeong.global.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import com.nyangmeong.global.file.FileRootResolver;

/**
 * [검사 - 로그 폴더 결정]
 */
class LogDirEnvironmentPostProcessorTest {

    private final LogDirEnvironmentPostProcessor processor = new LogDirEnvironmentPostProcessor();

    private static boolean windowsOrMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.startsWith("windows") || os.startsWith("mac");
    }

    @Test
    void 적은_값은_그대로_둔다() {
        MockEnvironment environment = new MockEnvironment().withProperty(LogDirEnvironmentPostProcessor.KEY, "build/my-logs");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty(LogDirEnvironmentPostProcessor.KEY)).isEqualTo("build/my-logs");
    }

    @Test
    void 비었으면_파일_저장_루트와_같은_규칙의_logs_폴더() {
        assumeTrue(windowsOrMac());
        MockEnvironment environment = new MockEnvironment();

        processor.postProcessEnvironment(environment, null);

        Path expected = FileRootResolver.resolve(null, false, System.getProperty("os.name"), System.getProperty("user.dir"),
                "logs", LogDirEnvironmentPostProcessor.KEY);
        assertThat(environment.getProperty(LogDirEnvironmentPostProcessor.KEY)).isEqualTo(expected.toString());
        assertThat(expected.toString().replace('\\', '/')).endsWith("ggorilab/logs");
    }

    @Test
    void Windows는_드라이브_루트_아래_Mac은_Users_Shared_아래() {
        assertThat(FileRootResolver.resolve(null, false, "Mac OS X", "/Users/a/backend", "logs", "app.log.dir"))
                .isEqualTo(Path.of("/Users/Shared/ggorilab/logs"));
        assumeTrue(System.getProperty("os.name").toLowerCase().startsWith("windows"));
        assertThat(FileRootResolver.resolve("", false, "Windows 11", "D:\\proj\\backend", "logs", "app.log.dir"))
                .isEqualTo(Path.of("D:\\home\\ggorilab\\logs"));
    }

    @Test
    void prod는_비었으면_지정하라는_오류() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThatThrownBy(() -> processor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("app.log.dir을 지정하세요");
    }
}
