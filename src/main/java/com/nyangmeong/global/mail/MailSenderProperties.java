package com.nyangmeong.global.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * [메일 발송 설정]
 * - app.mail.client : log(보내지 않고 로그만, 기본), smtp(실제 발송, spring.mail.* 계정 필요)
 * - app.mail.from : 보낸 사람 주소(smtp 일 때 필수), app.mail.from-name : 표시 이름
 * - 서버·계정·시간 제한은 Spring 표준 spring.mail.* 을 쓴다
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailSenderProperties {

    /** 발송기 종류 */
    private String client = "log";

    /** 보낸 사람 주소 */
    private String from;

    /** 보낸 사람 표시 이름 */
    private String fromName = "냥멍케어";
}
