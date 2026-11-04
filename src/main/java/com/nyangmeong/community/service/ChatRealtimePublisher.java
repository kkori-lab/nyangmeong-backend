package com.nyangmeong.community.service;

import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.nyangmeong.community.dto.ChatMessageSummary;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [채팅 실시간 전송]
 * - /user/queue/chat 구독자(회원 번호 기준, 모든 기기 연결)에게 MESSAGE·READ 전송
 * - 트랜잭션이 있으면 커밋 뒤에 보냄 -> 롤백된 메시지가 나가지 않음
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRealtimePublisher {

    public static final String TYPE_MESSAGE = "MESSAGE";
    public static final String TYPE_READ = "READ";
    private static final String QUEUE = "/queue/chat";

    private final SimpMessagingTemplate messagingTemplate;

    /** 새 메시지 이벤트 */
    public record MessageEvent(String type, Long otoChatRoomSn, ChatMessageSummary message) {
    }

    /** 읽음 이벤트 */
    public record ReadEvent(String type, Long otoChatRoomSn, Long mbrSn, Integer msgSeq) {
    }

    /**
     * 새 메시지 전송 (받는 사람 기준 mine 으로 각각)
     */
    public void publishMessage(Long otoChatRoomSn, List<Long> receiverMbrSns, ChatMessageSummary message) {
        afterCommit(() -> {
            for (Long receiverMbrSn : receiverMbrSns) {
                send(receiverMbrSn,
                        new MessageEvent(TYPE_MESSAGE, otoChatRoomSn, message.forViewer(receiverMbrSn)));
            }
        });
    }

    /**
     * 읽음 변경 전송
     */
    public void publishRead(Long otoChatRoomSn, List<Long> receiverMbrSns, Long readerMbrSn, Integer msgSeq) {
        afterCommit(() -> {
            for (Long receiverMbrSn : receiverMbrSns) {
                send(receiverMbrSn, new ReadEvent(TYPE_READ, otoChatRoomSn, readerMbrSn, msgSeq));
            }
        });
    }

    private void afterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    private void send(Long receiverMbrSn, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(String.valueOf(receiverMbrSn), QUEUE, payload);
        } catch (RuntimeException e) {
            /** 이미 커밋된 저장을 되돌릴 수 없어 전송 실패는 기록만 하고 요청은 성공 처리 */
            log.warn("채팅 실시간 전송 실패 mbrSn={}", receiverMbrSn, e);
        }
    }
}
