package com.nyangmeong.global.mail;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.notification.CertificationMailEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [인증번호 메일 리스너]
 * - 발행한 트랜잭션이 커밋되기 직전에 발송 : 실패하면 MAIL_SEND_FAILED 로 본 작업을 롤백한다
 *   (발송 실패를 성공으로 응답하지 않고, 실패한 요청이 재전송 간격·횟수에 세어지지 않는다, FN-ACC-001-04)
 * - 발송 동안 DB 연결을 잡고 있으므로 SMTP 시간 제한(spring.mail.properties.mail.smtp.*timeout)을 둔다
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CertificationMailListener {

    private final MailSender mailSender;

    /**
     * 인증번호 메일 이벤트 수신
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(CertificationMailEvent event) {
        try {
            mailSender.send(event.to(), event.mailTtl(), event.mailCn());
        } catch (RuntimeException e) {
            log.error("[Mail] 인증번호 메일 발송 실패 subject={}", event.mailTtl(), e);
            throw new CustomException(ErrorCode.MAIL_SEND_FAILED);
        }
    }
}
