package com.nyangmeong.global.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.SocketTimeoutException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.nyangmeong.global.external.ExternalApiCallRequest;
import com.nyangmeong.global.external.ExternalApiUsageService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * [검사 - SMTP 메일 발송기]
 * - 받는 사람·제목·본문(UTF-8)·보낸 사람 주소와 표시 이름이 메시지에 들어간다
 * - 발송 실패는 예외가 그대로 나간다 (호출한 쪽이 처리)
 * - 실제로 보내지 않고 JavaMailSender 를 대신한다
 * - 발송마다 메일(SYSC0020) 호출 로그를 새 트랜잭션으로 남긴다 : 성공·실패·시간초과, 저장 실패는 발송에 영향 없음
 */
class SmtpMailSenderTest {

    JavaMailSender javaMailSender = mock(JavaMailSender.class);
    MailSenderProperties properties = new MailSenderProperties();
    SmtpMailSender sender;

    @BeforeEach
    void setUp() {
        properties.setFrom("noreply@example.com");
        properties.setFromName("냥멍케어");
        when(javaMailSender.createMimeMessage()).thenAnswer(inv -> new JavaMailSenderImpl().createMimeMessage());
        sender = new SmtpMailSender(javaMailSender, properties);
    }

    private MimeMessage sent() {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        return captor.getValue();
    }

    @Test
    void 받는_사람과_제목과_본문을_담아_보낸다() throws Exception {
        sender.send("user@example.com", "[냥멍케어] 이메일 인증", "인증번호는 123456 입니다. 3분 안에 입력해 주세요.");

        MimeMessage message = sent();
        assertThat(message.getAllRecipients()).hasSize(1);
        assertThat(((InternetAddress) message.getAllRecipients()[0]).getAddress()).isEqualTo("user@example.com");
        assertThat(message.getSubject()).isEqualTo("[냥멍케어] 이메일 인증");
        assertThat(message.getContent().toString()).isEqualTo("인증번호는 123456 입니다. 3분 안에 입력해 주세요.");
    }

    @Test
    void 보낸_사람은_주소와_표시_이름을_쓴다() throws Exception {
        sender.send("user@example.com", "제목", "본문");

        InternetAddress from = (InternetAddress) sent().getFrom()[0];
        assertThat(from.getAddress()).isEqualTo("noreply@example.com");
        assertThat(from.getPersonal()).isEqualTo("냥멍케어");
    }

    @Test
    void 한글_본문은_UTF8로_보낸다() throws Exception {
        sender.send("user@example.com", "제목", "본문");

        MimeMessage message = sent();
        message.saveChanges();
        assertThat(message.getContentType().toLowerCase()).contains("text/plain").contains("utf-8");
    }

    @Test
    void 발송에_실패하면_예외가_그대로_나간다() {
        MailSendException failure = new MailSendException("연결 실패");
        doThrow(failure).when(javaMailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> sender.send("user@example.com", "제목", "본문")).isSameAs(failure);
    }

    private SmtpMailSender recordingSender(ExternalApiUsageService usageService) {
        return new SmtpMailSender(javaMailSender, properties, Optional.of(usageService));
    }

    private ExternalApiCallRequest recorded(ExternalApiUsageService usageService) {
        ArgumentCaptor<ExternalApiCallRequest> captor = ArgumentCaptor.forClass(ExternalApiCallRequest.class);
        verify(usageService).recordInNewTransaction(captor.capture());
        return captor.getValue();
    }

    @Test
    void 보내면_메일_성공_호출_로그를_남긴다() {
        ExternalApiUsageService usageService = mock(ExternalApiUsageService.class);

        recordingSender(usageService).send("user@example.com", "제목", "본문");

        ExternalApiCallRequest req = recorded(usageService);
        assertThat(req.extlApiSeCd()).isEqualTo("SYSC0020");
        assertThat(req.callRsltCd()).isEqualTo("SYSC0021");
        assertThat(req.callDt()).isNotNull();
        assertThat(req.rspnsMs()).isNotNull().isGreaterThanOrEqualTo(0L);
        assertThat(req.httpSttsVl()).isNull();
        assertThat(req.failRsn()).isNull();
    }

    @Test
    void 실패하면_예외_클래스_이름만_사유로_남기고_예외는_그대로_나간다() {
        ExternalApiUsageService usageService = mock(ExternalApiUsageService.class);
        MailAuthenticationException failure = new MailAuthenticationException("535 user@example.com 인증 실패");
        doThrow(failure).when(javaMailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> recordingSender(usageService).send("user@example.com", "제목", "본문"))
                .isSameAs(failure);

        ExternalApiCallRequest req = recorded(usageService);
        assertThat(req.extlApiSeCd()).isEqualTo("SYSC0020");
        assertThat(req.callRsltCd()).isEqualTo("SYSC0022");
        assertThat(req.failRsn()).isEqualTo("MailAuthenticationException");
    }

    @Test
    void 원인에_소켓_시간초과가_있으면_시간초과로_남긴다() {
        ExternalApiUsageService usageService = mock(ExternalApiUsageService.class);
        MessagingException messaging = new MessagingException("연결 실패", new SocketTimeoutException("Read timed out"));
        MailSendException failure = new MailSendException("발송 실패", messaging);
        doThrow(failure).when(javaMailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> recordingSender(usageService).send("user@example.com", "제목", "본문"))
                .isSameAs(failure);

        ExternalApiCallRequest req = recorded(usageService);
        assertThat(req.callRsltCd()).isEqualTo("SYSC0023");
        assertThat(req.failRsn()).isEqualTo("MailSendException");
    }

    @Test
    void 호출_로그_저장이_실패해도_발송은_성공한다() {
        ExternalApiUsageService usageService = mock(ExternalApiUsageService.class);
        when(usageService.recordInNewTransaction(any())).thenThrow(new IllegalStateException("DB 오류"));

        recordingSender(usageService).send("user@example.com", "제목", "본문");

        verify(javaMailSender).send(any(MimeMessage.class));
    }
}
