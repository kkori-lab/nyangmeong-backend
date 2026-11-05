package com.nyangmeong.global.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * [검사 - SMTP 메일 발송기]
 * - 받는 사람·제목·본문(UTF-8)·보낸 사람 주소와 표시 이름이 메시지에 들어간다
 * - 발송 실패는 예외가 그대로 나간다 (호출한 쪽이 처리)
 * - 실제로 보내지 않고 JavaMailSender 를 대신한다
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
}
