package com.nyangmeong.global.log;

import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;

import com.nyangmeong.global.file.FileRootResolver;

/**
 * [로그 폴더 결정]
 * - logback-spring.xml 의 LOG_DIR 은 app.log.dir 을 읽는다. logback 이 뜨기 전에 값을 채워야 해서 EnvironmentPostProcessor 로 둔다
 * - app.log.dir 을 적었으면 그 값, 비었으면 파일 저장 루트와 같은 규칙 : Windows = 드라이브 루트 아래 home/ggorilab/logs, Mac = /Users/Shared/ggorilab/logs
 * - prod 이거나 그 밖의 OS 는 지정하라는 오류 (운영은 application-prod.properties 에 적는다)
 * - META-INF/spring.factories 에 등록한다
 */
public class LogDirEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String KEY = "app.log.dir";

    private static final String FOLDER = "logs";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String configured = environment.getProperty(KEY);
        if (configured != null && !configured.isBlank()) {
            return;
        }
        boolean prod = environment.acceptsProfiles(Profiles.of("prod"));
        String dir = FileRootResolver.resolve(null, prod, System.getProperty("os.name"), System.getProperty("user.dir"), FOLDER, KEY)
                .toString();
        environment.getPropertySources().addLast(new MapPropertySource("logDirDefault", Map.of(KEY, dir)));
    }
}
