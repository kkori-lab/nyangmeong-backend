package com.nyangmeong.global.mail;

import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.nyangmeong.global.external.ExternalApiCallRequest;
import com.nyangmeong.global.external.ExternalApiCallResult;
import com.nyangmeong.global.external.ExternalApiType;
import com.nyangmeong.global.external.ExternalApiUsageService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * [SMTP 메일 발송기]
 * - app.mail.client=smtp 일 때만 쓴다 (기본은 LoggingMailSender)
 * - 본문은 일반 텍스트 UTF-8, 실패는 MailException 을 그대로 던진다 (호출한 쪽이 처리)
 * - 받는 주소는 로그에 남기지 않음 (docs/convention/common.md)
 * - 발송 한 번마다 외부 API 호출 로그(메일 SYSC0020)를 남긴다 (FN-ADM-112-01·03)
 *   : 메시지 만들기 실패는 호출 전이라 남기지 않는다
 *   : 실패 사유는 예외 클래스 이름만 (SMTP 오류 메시지에 받는 주소가 들어갈 수 있다)
 *   : 로그 저장 실패는 발송 결과를 바꾸지 않는다
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.client", havingValue = "smtp")
public class SmtpMailSender implements MailSender {

    private final JavaMailSender javaMailSender;
    private final MailSenderProperties properties;
    private final Optional<ExternalApiUsageService> externalApiUsageService;

    @Autowired
    public SmtpMailSender(JavaMailSender javaMailSender, MailSenderProperties properties,
            Optional<ExternalApiUsageService> externalApiUsageService) {
        this.javaMailSender = javaMailSender;
        this.properties = properties;
        this.externalApiUsageService = externalApiUsageService;
    }

    /**
     * 호출 로그 없이 쓸 때 (사용량 서비스가 없는 단위 검사)
     */
    public SmtpMailSender(JavaMailSender javaMailSender, MailSenderProperties properties) {
        this(javaMailSender, properties, Optional.empty());
    }

    @Override
    public void send(String to, String subject, String content) {
        MimeMessage message = createMessage(to, subject, content);
        LocalDateTime calledAt = LocalDateTime.now();
        long startedAt = System.currentTimeMillis();
        try {
            javaMailSender.send(message);
        } catch (RuntimeException e) {
            record(calledAt, startedAt, isTimeout(e) ? ExternalApiCallResult.TIMEOUT : ExternalApiCallResult.FAIL,
                    e.getClass().getSimpleName());
            throw e;
        }
        record(calledAt, startedAt, ExternalApiCallResult.SUCCESS, null);
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

    /**
     * 원인 사슬에 소켓 시간초과가 있으면 시간초과
     */
    private boolean isTimeout(Throwable e) {
        for (Throwable cause = e; cause != null; cause = nextCause(cause)) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    /**
     * MessagingException 은 원인을 getNextException 에도 담는다
     */
    private Throwable nextCause(Throwable cause) {
        if (cause.getCause() != null && cause.getCause() != cause) {
            return cause.getCause();
        }
        if (cause instanceof MessagingException me) {
            return me.getNextException();
        }
        return null;
    }

    /**
     * 호출 로그 저장 : 본 작업이 롤백돼도 남게 새 트랜잭션
     */
    private void record(LocalDateTime calledAt, long startedAt, ExternalApiCallResult result, String failRsn) {
        externalApiUsageService.ifPresent(service -> {
            try {
                service.recordInNewTransaction(new ExternalApiCallRequest(ExternalApiType.MAIL.getCode(),
                        result.getCode(), calledAt, System.currentTimeMillis() - startedAt, null, null, null,
                        failRsn));
            } catch (RuntimeException e) {
                log.warn("[Mail] 호출 로그 저장에 실패했습니다: {}", e.getClass().getSimpleName());
            }
        });
    }
}
