package com.nyangmeong.global.mail;

/**
 * [메일 발송기]
 * - 실패 시 예외 -> 호출한 쪽이 이력·재시도 처리
 * - app.mail.client : log(기본) -> LoggingMailSender, smtp -> SmtpMailSender
 */
public interface MailSender {

    /**
     * 메일 발송
     */
    void send(String to, String subject, String content);
}
