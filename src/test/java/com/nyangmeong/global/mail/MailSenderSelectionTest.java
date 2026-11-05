package com.nyangmeong.global.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * [검사 - 메일 발송기 고르기]
 * - app.mail.client 가 없거나 log 면 LoggingMailSender 만, smtp 면 SmtpMailSender 만 뜬다
 * - 설정을 받지 않은 로컬에서 실제 사람에게 메일이 나가지 않게 기본은 log
 */
class MailSenderSelectionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
            .withBean(MailSenderProperties.class)
            .withUserConfiguration(LoggingMailSender.class, SmtpMailSender.class);

    @Test
    void 설정이_없으면_로그_발송기만_뜬다() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(MailSender.class);
            assertThat(context).hasSingleBean(LoggingMailSender.class);
            assertThat(context).doesNotHaveBean(SmtpMailSender.class);
        });
    }

    @Test
    void log면_로그_발송기만_뜬다() {
        runner.withPropertyValues("app.mail.client=log").run(context -> {
            assertThat(context).hasSingleBean(LoggingMailSender.class);
            assertThat(context).doesNotHaveBean(SmtpMailSender.class);
        });
    }

    @Test
    void smtp면_SMTP_발송기만_뜬다() {
        runner.withPropertyValues("app.mail.client=smtp").run(context -> {
            assertThat(context).hasSingleBean(MailSender.class);
            assertThat(context).hasSingleBean(SmtpMailSender.class);
            assertThat(context).doesNotHaveBean(LoggingMailSender.class);
        });
    }
}
