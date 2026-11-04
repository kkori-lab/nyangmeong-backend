package com.nyangmeong.global.security.stomp;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;

/**
 * [STOMP 수신 프레임 검사]
 * - CONNECT : 인증된 사용자만
 * - SUBSCRIBE : 내 개인 큐(/user/queue/chat)만 -> 남의 큐나 다른 목적지는 구독 불가
 * - SEND : 보는 방 알림(/app/chat/view)만
 */
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    public static final String SUBSCRIBE_DESTINATION = "/user/queue/chat";
    public static final String VIEW_DESTINATION = "/app/chat/view";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }
        switch (command) {
            case CONNECT, STOMP -> {
                if (accessor.getUser() == null) {
                    throw new MessageDeliveryException(message, "인증이 필요합니다.");
                }
            }
            case SUBSCRIBE -> {
                if (accessor.getUser() == null || !SUBSCRIBE_DESTINATION.equals(accessor.getDestination())) {
                    throw new MessageDeliveryException(message, "구독할 수 없는 목적지입니다.");
                }
            }
            case SEND -> {
                if (accessor.getUser() == null || !VIEW_DESTINATION.equals(accessor.getDestination())) {
                    throw new MessageDeliveryException(message, "보낼 수 없는 목적지입니다.");
                }
            }
            default -> {
            }
        }
        return message;
    }
}
