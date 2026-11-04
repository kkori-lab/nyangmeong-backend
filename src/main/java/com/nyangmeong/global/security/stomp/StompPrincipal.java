package com.nyangmeong.global.security.stomp;

import java.security.Principal;

/**
 * [STOMP 연결 사용자]
 * - name 은 MBR_SN, convertAndSendToUser 의 사용자 식별자로 쓴다 -> 같은 회원의 모든 기기 연결로 전달
 */
public record StompPrincipal(String name) implements Principal {

    @Override
    public String getName() {
        return name;
    }

    public static StompPrincipal of(Long mbrSn) {
        return new StompPrincipal(String.valueOf(mbrSn));
    }
}
