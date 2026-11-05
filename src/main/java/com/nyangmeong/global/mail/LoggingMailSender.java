package com.nyangmeong.global.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * [가짜 메일 발송기]
 * - app.mail.client 가 log 이거나 없을 때 쓴다 : 보내지 않고 로그만 (설정을 받지 않은 로컬에서 실제 사람에게 나가지 않게)
 * - 받는 주소는 로그에 남기지 않음 (docs/convention/common.md)
 * - 인증번호를 눈으로 확인하도록 본문은 debug 로만 남김 : 실제 발송기로 바꾸면 사라진다
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.client", havingValue = "log", matchIfMissing = true)
public class LoggingMailSender implements MailSender {

    @Override
    public void send(String to, String subject, String content) {
        log.info("[Mail] 가짜 발송 subject={}", subject);
        log.debug("[Mail] 가짜 발송 본문={}", content);
    }
}
