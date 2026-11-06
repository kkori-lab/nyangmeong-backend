package com.nyangmeong.global.mail;

/**
 * [메일 받을 수 있는 도메인 확인]
 * - 인증번호를 보내기 전에 이메일 도메인이 메일을 받을 수 있는지 본다
 * - 아이디(@ 앞)가 실제로 있는지는 알 수 없다
 */
public interface MailDomainChecker {

    /**
     * 도메인이 메일을 받을 수 없다고 확실할 때만 false, 조회가 실패하면 true(정상 가입을 막지 않는다)
     */
    boolean isReceivable(String domain);
}
