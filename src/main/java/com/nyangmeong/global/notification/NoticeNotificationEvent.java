package com.nyangmeong.global.notification;

import java.util.Objects;

/**
 * [공지 전체 알림 발행 이벤트]
 * - EventPublisher.publish() 로만 발행 -> notification 도메인이 커밋 뒤 정상 회원 전체 INAPP_NOTI 를 한 번에 저장
 * - 회원마다 NotificationEvent 를 발행하지 않는다 (회원 수만큼 트랜잭션이 생겨 느림)
 * - 앱 알림 채널을 끈 회원은 저장 단계에서 미생성
 */
public record NoticeNotificationEvent(String notiTtl, String notiCn, String mvmnPath) {

    public NoticeNotificationEvent {
        Objects.requireNonNull(notiTtl, "notiTtl");
    }
}
