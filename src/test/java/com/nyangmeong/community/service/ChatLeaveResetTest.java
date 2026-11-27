package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.nyangmeong.community.domain.CareMatch;
import com.nyangmeong.community.domain.CareRecruitment;
import com.nyangmeong.community.domain.ChatParticipant;
import com.nyangmeong.community.domain.ChatRoomDetail;
import com.nyangmeong.community.dto.CareMatchCreateRequest;
import com.nyangmeong.community.dto.ChatRoomResponse;
import com.nyangmeong.community.mapper.CareMatchMapper;
import com.nyangmeong.community.mapper.CareRecruitmentMapper;
import com.nyangmeong.community.mapper.ChatMessageMapper;
import com.nyangmeong.community.mapper.ChatRoomMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.notification.EventPublisher;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.validation.ProfanityFilter;
import com.nyangmeong.notification.service.NotificationService;

/**
 * [검사 - 나간 사람 화면 리셋과 상대 나감 표시]
 * - 나가기: 안내 메시지까지 남긴 뒤 보이는 시작 메시지를 올린다. 확정 거래 중이면 409(회귀), 거래 완료 방도 리셋
 * - 메시지 조회·사진: 요청한 회원 기준으로 조회한다
 * - 매칭 요청: 상대가 나간 방이면 409
 * - 방 응답: 보는 사람 기준 상대 참여 상태로 ctptLeft
 */
class ChatLeaveResetTest {

    private static final Long OWNER = 1L;
    private static final Long SITTER = 2L;
    private static final Long PST = 7L;
    private static final Long RCRT = 5L;
    private static final Long ROOM = 11L;
    private static final String JOINED = "CHTC0003";
    private static final String LEFT = "CHTC0004";

    private final ChatRoomMapper chatRoomMapper = mock(ChatRoomMapper.class);
    private final ChatMessageMapper chatMessageMapper = mock(ChatMessageMapper.class);
    private final CareMatchMapper careMatchMapper = mock(CareMatchMapper.class);
    private final CareRecruitmentMapper careRecruitmentMapper = mock(CareRecruitmentMapper.class);
    private final CareMatchService careMatchServiceMock = mock(CareMatchService.class);
    private final ChatMessageService chatMessageServiceMock = mock(ChatMessageService.class);
    private final FileStore fileStore = mock(FileStore.class);

    private final ChatRoomService roomService = new ChatRoomService(chatRoomMapper, careRecruitmentMapper,
            new ChatRoomGuard(chatRoomMapper), careMatchServiceMock, chatMessageServiceMock,
            mock(ChatRealtimePublisher.class), mock(FieldCryptoService.class), fileStore, mock(NotificationService.class));
    private final ChatMessageService messageService = new ChatMessageService(chatRoomMapper, chatMessageMapper,
            careMatchMapper, new ChatRoomGuard(chatRoomMapper), mock(ChatRealtimePublisher.class), fileStore,
            mock(FieldCryptoService.class), mock(ProfanityFilter.class));
    private final CareMatchService matchService = new CareMatchService(careMatchMapper, careRecruitmentMapper,
            chatRoomMapper, new ChatRoomGuard(chatRoomMapper), chatMessageServiceMock, mock(ChatViewRegistry.class),
            mock(EventPublisher.class));

    private ChatRoomDetail room(String mtchDlngSttsCd) {
        ChatRoomDetail detail = new ChatRoomDetail();
        detail.setOtoChatRoomSn(ROOM);
        detail.setPstSn(PST);
        detail.setCtptMbrSn(SITTER);
        detail.setOwnerMbrSn(OWNER);
        detail.setChatRoomSttsCd("CHTC0001");
        detail.setPetPrtcRcrtSn(RCRT);
        detail.setRcrtSttsCd("PCAC0001");
        detail.setMtchDlngSttsCd(mtchDlngSttsCd);
        detail.setPetPrtcMtchSn(mtchDlngSttsCd == null ? null : 21L);
        return detail;
    }

    private ChatParticipant participant(String ptcpSttsCd) {
        ChatParticipant participant = new ChatParticipant();
        participant.setChatPrtpntSn(100L);
        participant.setPtcpSttsCd(ptcpSttsCd);
        participant.setLastPrslMsgSeq(0);
        participant.setVsblBgngMsgSeq(0);
        return participant;
    }

    private void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    @Test
    void 나가면_나감_안내를_남긴_뒤_보이는_시작_메시지를_올린다() {
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(room(null));
        when(chatRoomMapper.selectParticipant(ROOM, SITTER)).thenReturn(participant(JOINED));
        when(chatRoomMapper.selectParticipant(ROOM, OWNER)).thenReturn(participant(JOINED));

        roomService.leave(SITTER, ROOM);

        InOrder order = inOrder(chatRoomMapper, chatMessageServiceMock);
        order.verify(chatRoomMapper).updateParticipantStatus(ROOM, SITTER, LEFT);
        order.verify(chatMessageServiceMock).createSystemMessage(ROOM, "상대가 채팅방을 나갔어요.");
        order.verify(chatRoomMapper).updateVisibleStart(ROOM, SITTER);
        verify(chatRoomMapper, never()).updateVisibleStart(ROOM, OWNER);
    }

    @Test
    void 수락_대기_매칭을_취소하며_나가도_리셋한다() {
        ChatRoomDetail detail = room("PCAC0012");
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(detail);
        when(chatRoomMapper.selectParticipant(ROOM, SITTER)).thenReturn(participant(JOINED));
        when(careMatchServiceMock.cancelOnLeave(SITTER, detail)).thenReturn(true);

        roomService.leave(SITTER, ROOM);

        InOrder order = inOrder(careMatchServiceMock, chatRoomMapper);
        order.verify(careMatchServiceMock).cancelOnLeave(SITTER, detail);
        order.verify(chatRoomMapper).updateVisibleStart(ROOM, SITTER);
    }

    @Test
    void 거래_완료_방도_나가면_리셋한다() {
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(room("PCAC0016"));
        when(chatRoomMapper.selectParticipant(ROOM, OWNER)).thenReturn(participant(JOINED));

        roomService.leave(OWNER, ROOM);

        verify(chatRoomMapper).updateParticipantStatus(ROOM, OWNER, LEFT);
        verify(chatRoomMapper).updateVisibleStart(ROOM, OWNER);
    }

    @Test
    void 확정_거래_중에는_나갈_수_없고_리셋하지_않는다() {
        for (String sttsCd : List.of("PCAC0013", "PCAC0014", "PCAC0015")) {
            when(chatRoomMapper.selectDetail(ROOM)).thenReturn(room(sttsCd));
            when(chatRoomMapper.selectParticipant(ROOM, SITTER)).thenReturn(participant(JOINED));

            assertError(() -> roomService.leave(SITTER, ROOM), ErrorCode.CHAT_LEAVE_IN_DEAL);
        }
        verify(chatRoomMapper, never()).updateVisibleStart(any(), any());
    }

    @Test
    void 이미_나간_방을_다시_나가면_리셋하지_않는다() {
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(room(null));
        when(chatRoomMapper.selectParticipant(ROOM, SITTER)).thenReturn(participant(LEFT));

        roomService.leave(SITTER, ROOM);

        verify(chatRoomMapper, never()).updateVisibleStart(any(), any());
    }

    @Test
    void 메시지_조회는_요청한_회원_기준으로_한다() {
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(room(null));
        when(chatMessageMapper.selectList(ROOM, SITTER, null, 31)).thenReturn(List.of());

        assertThat(messageService.search(SITTER, ROOM, null, null).content()).isEmpty();

        verify(chatMessageMapper).selectList(ROOM, SITTER, null, 31);
    }

    @Test
    void 나가기_전_사진은_요청한_회원_기준으로_막는다() {
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(room(null));
        when(chatRoomMapper.existsPhoto(ROOM, SITTER, 600L)).thenReturn(false);
        when(chatRoomMapper.existsPhoto(ROOM, OWNER, 600L)).thenReturn(true);

        assertError(() -> roomService.getImage(SITTER, ROOM, 600L), ErrorCode.FILE_NOT_FOUND);
        roomService.getImage(OWNER, ROOM, 600L);

        verify(fileStore).load(600L);
    }

    @Test
    void 상대가_나간_방에서는_매칭을_요청할_수_없다() {
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(room(null));
        when(chatRoomMapper.selectParticipant(ROOM, OWNER)).thenReturn(participant(JOINED));
        when(chatRoomMapper.selectParticipant(ROOM, SITTER)).thenReturn(participant(LEFT));
        CareRecruitment recruitment = new CareRecruitment();
        recruitment.setPetPrtcRcrtSn(RCRT);
        recruitment.setPstSn(PST);
        recruitment.setRcrtSttsCd("PCAC0001");
        when(careRecruitmentMapper.selectForUpdate(RCRT)).thenReturn(recruitment);

        assertError(() -> matchService.request(OWNER, new CareMatchCreateRequest(ROOM, "req-left")),
                ErrorCode.CARE_MATCH_COUNTERPART_LEFT);

        verify(careMatchMapper, never()).insertMatch(any(CareMatch.class));
        verify(careMatchMapper, never()).updateRequestAgain(any(), any(), any(), any());
    }

    @Test
    void 방_응답의_상대_나감은_보는_사람_기준이다() {
        ChatRoomDetail detail = room(null);
        detail.setOwnerPtcpSttsCd(JOINED);
        detail.setApplicantPtcpSttsCd(LEFT);

        ChatRoomResponse forOwner = ChatRoomResponse.of(detail, OWNER, false);
        ChatRoomResponse forSitter = ChatRoomResponse.of(detail, SITTER, false);

        assertThat(forOwner.ctptLeft()).isTrue();
        assertThat(forSitter.ctptLeft()).isFalse();
        assertThat(forOwner.withCnclDmnd(null).ctptLeft()).isTrue();
    }
}
