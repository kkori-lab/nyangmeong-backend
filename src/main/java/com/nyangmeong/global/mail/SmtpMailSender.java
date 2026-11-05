package com.nyangmeong.global.mail;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [SMTP 메일 발송기]
 * - app.mail.client=smtp 일 때만 쓴다 (기본은 LoggingMailSender)
 * - 본문은 일반 텍스트 UTF-8, 실패는 MailException 을 그대로 던진다 (호출한 쪽이 처리)
 * - 받는 주소는 로그에 남기지 않음 (docs/convention/common.md)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail.client", havingValue = "smtp")
public class SmtpMailSender implements MailSender {

    private final JavaMailSender javaMailSender;
    private final MailSenderProperties properties;

    @Override
    public void send(String to, String subject, String content) {
        javaMailSender.send(createMessage(to, subject, content));
        log.info("[Mail] 발송 subject={}", subject);
    }

    private MimeMessage createMessage(String to, String subject, String content) {
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.getFrom(), properties.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, false);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailPreparationException("메일 메시지를 만들지 못했습니다.", e);
        }
        return message;
    }
}
