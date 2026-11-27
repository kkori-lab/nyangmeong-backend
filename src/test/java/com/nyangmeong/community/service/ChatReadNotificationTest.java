package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.nyangmeong.auth.service.AuthService;
import com.nyangmeong.community.domain.ChatParticipant;
import com.nyangmeong.community.domain.ChatRoomDetail;
import com.nyangmeong.community.dto.ChatRoomReadRequest;
import com.nyangmeong.community.mapper.CareRecruitmentMapper;
import com.nyangmeong.community.mapper.ChatRoomMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.notification.mapper.NotificationMapper;
import com.nyangmeong.notification.service.NotificationService;

/**
 * [검사 - 채팅방 읽음 때 그 방 거래 알림 읽음]
 * - 채팅방 읽음은 그 방 이동 경로의 내 알림을 읽음 처리한다(읽음 위치가 안 바뀌어도)
 * - 참여자가 아니면 알림을 건드리지 않는다
 * - 알림 서비스는 회원·경로를 그대로 Mapper 에 넘긴다
 */
class ChatReadNotificationTest {

    private static final Long OWNER = 1L;
    private static final Long SITTER = 2L;
    private static final Long ROOM = 11L;
    private static final String ROOM_PATH = "/chat?otoChatRoomSn=11";

    private final ChatRoomMapper chatRoomMapper = mock(ChatRoomMapper.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final ChatRoomService service = new ChatRoomService(chatRoomMapper, mock(CareRecruitmentMapper.class),
            new ChatRoomGuard(chatRoomMapper), mock(CareMatchService.class), mock(ChatMessageService.class),
            mock(ChatRealtimePublisher.class), mock(FieldCryptoService.class), mock(FileStore.class),
            notificationService);

    private ChatRoomDetail room() {
        ChatRoomDetail detail = new ChatRoomDetail();
        detail.setOtoChatRoomSn(ROOM);
        detail.setPstSn(7L);
        detail.setCtptMbrSn(SITTER);
        detail.setOwnerMbrSn(OWNER);
        detail.setChatRoomSttsCd("CHTC0001");
        return detail;
    }

    @Test
    void 채팅방을_읽으면_그_방_거래_알림을_읽음_처리한다() {
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(room());
        when(chatRoomMapper.updateLastRead(ROOM, OWNER, 5)).thenReturn(1);
        ChatParticipant participant = new ChatParticipant();
        participant.setLastPrslMsgSeq(5);
        when(chatRoomMapper.selectParticipant(ROOM, OWNER)).thenReturn(participant);

        service.read(OWNER, ROOM, new ChatRoomReadRequest(5));

        verify(notificationService).markReadByPath(OWNER, ROOM_PATH);
    }

    @Test
    void 이미_다_읽은_방을_다시_열어도_남은_거래_알림을_읽음_처리한다() {
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(room());
        when(chatRoomMapper.updateLastRead(ROOM, SITTER, 5)).thenReturn(0);

        service.read(SITTER, ROOM, new ChatRoomReadRequest(5));

        verify(notificationService).markReadByPath(SITTER, ROOM_PATH);
    }

    @Test
    void 참여자가_아니면_알림을_건드리지_않는다() {
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(room());

        assertThatThrownBy(() -> service.read(99L, ROOM, new ChatRoomReadRequest(1)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CHAT_ROOM_ACCESS_DENIED));
        verify(notificationService, never()).markReadByPath(any(), any());
    }

    @Test
    void 알림_서비스는_회원과_경로로_읽음_처리하고_건수를_돌려준다() {
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        NotificationService real = new NotificationService(notificationMapper, mock(AuthService.class));
        when(notificationMapper.updateReadByMvmnPath(OWNER, ROOM_PATH)).thenReturn(3);

        assertThat(real.markReadByPath(OWNER, ROOM_PATH)).isEqualTo(3);
        verify(notificationMapper).updateReadByMvmnPath(OWNER, ROOM_PATH);
    }
}
