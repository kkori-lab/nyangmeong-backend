package com.nyangmeong.global.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.notification.CertificationMailEvent;

/**
 * [검사 - 인증번호 메일 리스너]
 * - 성공하면 그대로, 발송이 실패하면 MAIL_SEND_FAILED 로 본 작업을 롤백하게 한다
 * - 커밋 직전(BEFORE_COMMIT)에 실행된다 : 커밋 뒤면 예외가 응답에 닿지 않는다
 */
class CertificationMailListenerTest {

    MailSender mailSender = mock(MailSender.class);
    CertificationMailListener listener = new CertificationMailListener(mailSender);
    CertificationMailEvent event = new CertificationMailEvent("user@example.com", "[냥멍케어] 이메일 인증", "인증번호는 123456 입니다.");

    @Test
    void 발송에_성공하면_받는_사람과_제목과_본문을_넘긴다() {
        listener.handle(event);

        verify(mailSender).send("user@example.com", "[냥멍케어] 이메일 인증", "인증번호는 123456 입니다.");
    }

    @Test
    void 발송에_실패하면_MAIL_SEND_FAILED다() {
        doThrow(new MailSendException("연결 실패")).when(mailSender).send("user@example.com", "[냥멍케어] 이메일 인증",
                "인증번호는 123456 입니다.");

        Throwable thrown = catchThrowable(() -> listener.handle(event));

        assertThat(thrown).isInstanceOf(CustomException.class);
        assertThat(((CustomException) thrown).getErrorCode()).isEqualTo(ErrorCode.MAIL_SEND_FAILED);
    }

    @Test
    void 커밋_직전에_실행된다() throws Exception {
        Method handle = CertificationMailListener.class.getMethod("handle", CertificationMailEvent.class);

        TransactionalEventListener annotation = handle.getAnnotation(TransactionalEventListener.class);

        assertThat(annotation.phase()).isEqualTo(TransactionPhase.BEFORE_COMMIT);
    }
}
