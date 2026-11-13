package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.nyangmeong.community.domain.CareMatch;
import com.nyangmeong.community.domain.ChatMessage;
import com.nyangmeong.community.domain.ChatMessagePhoto;
import com.nyangmeong.community.domain.ChatParticipant;
import com.nyangmeong.community.domain.ChatRoomDetail;
import com.nyangmeong.community.dto.ChatMessageCreateRequest;
import com.nyangmeong.community.dto.ChatMessageResponse;
import com.nyangmeong.community.dto.ChatMessageSliceResponse;
import com.nyangmeong.community.dto.ChatMessageSummary;
import com.nyangmeong.community.mapper.CareMatchMapper;
import com.nyangmeong.community.mapper.ChatMessageMapper;
import com.nyangmeong.community.mapper.ChatRoomMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.security.crypto.FieldCryptoService;

/**
 * [검사 - 펫돌봄 채팅 메시지]
 */
class ChatMessageServiceTest {

    private static final Long OWNER = 1L;
    private static final Long SITTER = 2L;
    private static final Long ROOM = 11L;

    private final ChatRoomMapper chatRoomMapper = mock(ChatRoomMapper.class);
    private final ChatMessageMapper chatMessageMapper = mock(ChatMessageMapper.class);
    private final CareMatchMapper careMatchMapper = mock(CareMatchMapper.class);
    private final ChatRealtimePublisher chatRealtimePublisher = mock(ChatRealtimePublisher.class);
    private final FileStore fileStore = mock(FileStore.class);
    private final FieldCryptoService fieldCryptoService = mock(FieldCryptoService.class);
    private final ChatMessageService service = new ChatMessageService(chatRoomMapper, chatMessageMapper,
            careMatchMapper, new ChatRoomGuard(chatRoomMapper), chatRealtimePublisher, fileStore,
            fieldCryptoService);

    @BeforeEach
    void stubCommon() {
        ChatRoomDetail room = new ChatRoomDetail();
        room.setOtoChatRoomSn(ROOM);
        room.setOwnerMbrSn(OWNER);
        room.setCtptMbrSn(SITTER);
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(room);
        stubParticipant("CHTC0003");
        when(chatRoomMapper.selectParticipantMbrSns(ROOM, "CHTC0003")).thenReturn(List.of(OWNER, SITTER));
        when(fieldCryptoService.encrypt(anyString())).thenAnswer(inv -> "ENC:" + inv.getArgument(0));
        when(fieldCryptoService.decrypt(anyString()))
                .thenAnswer(inv -> inv.<String>getArgument(0).substring("ENC:".length()));
        when(chatMessageMapper.selectNextMsgSeqForUpdate(ROOM)).thenReturn(5);
        doAnswer(inv -> {
            inv.<ChatMessage>getArgument(0).setChatMsgSn(77L);
            return 1;
        }).when(chatMessageMapper).insertMessage(any(ChatMessage.class));
        AtomicLong fileSn = new AtomicLong(500);
        when(fileStore.store(any(MultipartFile.class), any(), any(), anyString())).thenAnswer(inv -> fileSn.getAndIncrement());
    }

    private void stubParticipant(String ptcpSttsCd) {
        ChatParticipant participant = new ChatParticipant();
        participant.setPtcpSttsCd(ptcpSttsCd);
        when(chatRoomMapper.selectParticipant(eq(ROOM), anyLong())).thenReturn(participant);
    }

    private ChatMessageCreateRequest text(String msgCn, String idmptDmndId) {
        return new ChatMessageCreateRequest("CHTC0005", msgCn, null, null, null, idmptDmndId);
    }

    private ChatMessageCreateRequest photos(int count) {
        List<MultipartFile> images = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            images.add(new MockMultipartFile("images", "a" + i + ".jpg", "image/jpeg", new byte[] { 1 }));
        }
        return new ChatMessageCreateRequest("CHTC0006", null, null, null, images, "photo-1");
    }

    private ChatMessageCreateRequest address(String shrnAddr, String dtlAddr) {
        return new ChatMessageCreateRequest("CHTC0007", null, shrnAddr, dtlAddr, null, "addr-1");
    }

    private void stubMatchStatus(String sttsCd) {
        CareMatch match = new CareMatch();
        match.setMtchDlngSttsCd(sttsCd);
        when(careMatchMapper.selectByRoomForUpdate(ROOM)).thenReturn(match);
    }

    private void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    @Test
    void 텍스트는_암호화해_다음_순서로_저장하고_두_참여자에게_실시간으로_보낸다() {
        ChatMessageResponse res = service.send(OWNER, ROOM, text("  안녕하세요  ", "m-1"));

        assertThat(res.created()).isTrue();
        assertThat(res.msgSeq()).isEqualTo(5);
        assertThat(res.mine()).isTrue();
        assertThat(res.msgCn()).isEqualTo("안녕하세요");
        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageMapper).insertMessage(saved.capture());
        assertThat(saved.getValue().getMsgCnEnc()).isEqualTo("ENC:안녕하세요");
        assertThat(saved.getValue().getMsgTypeCd()).isEqualTo("CHTC0005");
        assertThat(saved.getValue().getMbrSn()).isEqualTo(OWNER);
        verify(chatRoomMapper).selectRoomForUpdate(ROOM);
        ArgumentCaptor<ChatMessageSummary> pushed = ArgumentCaptor.forClass(ChatMessageSummary.class);
        verify(chatRealtimePublisher).publishMessage(eq(ROOM), eq(List.of(OWNER, SITTER)), pushed.capture());
        assertThat(pushed.getValue().getChatMsgSn()).isEqualTo(77L);
    }

    @Test
    void 공백만_있는_텍스트는_거부한다() {
        assertError(() -> service.send(OWNER, ROOM, text("   ", "m-1")), ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.send(OWNER, ROOM, text(null, "m-1")), ErrorCode.INVALID_INPUT_VALUE);
        verify(chatMessageMapper, never()).insertMessage(any());
    }

    @Test
    void 직접_보낼_수_없는_유형은_거부한다() {
        for (String typeCd : List.of("CHTC0008", "CHTC0009", "CHTC9999")) {
            assertError(() -> service.send(OWNER, ROOM,
                    new ChatMessageCreateRequest(typeCd, "안녕", null, null, null, "m-1")),
                    ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Test
    void 같은_요청_ID_재전송은_저장하지_않고_기존_메시지를_돌려준다() {
        ChatMessage existing = new ChatMessage();
        existing.setChatMsgSn(70L);
        existing.setOtoChatRoomSn(ROOM);
        existing.setMbrSn(OWNER);
        existing.setMsgTypeCd("CHTC0005");
        existing.setMsgSeq(4);
        existing.setMsgCnEnc("ENC:처음 보낸 글");
        when(chatMessageMapper.selectByIdmptDmndIdForShare("m-1")).thenReturn(existing);

        ChatMessageResponse res = service.send(OWNER, ROOM, text("다시 보낸 글", "m-1"));

        assertThat(res.created()).isFalse();
        assertThat(res.chatMsgSn()).isEqualTo(70L);
        assertThat(res.msgCn()).isEqualTo("처음 보낸 글");
        verify(chatMessageMapper, never()).insertMessage(any());
        verify(chatRealtimePublisher, never()).publishMessage(anyLong(), anyList(), any());
    }

    @Test
    void 다른_사람이_쓴_요청_ID는_거부한다() {
        ChatMessage existing = new ChatMessage();
        existing.setOtoChatRoomSn(ROOM);
        existing.setMbrSn(SITTER);
        when(chatMessageMapper.selectByIdmptDmndIdForShare("m-1")).thenReturn(existing);

        assertError(() -> service.send(OWNER, ROOM, text("안녕", "m-1")), ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 사진은_열_장까지_순서대로_저장한다() {
        service.send(OWNER, ROOM, photos(10));

        ArgumentCaptor<ChatMessagePhoto> saved = ArgumentCaptor.forClass(ChatMessagePhoto.class);
        verify(chatMessageMapper, times(10)).insertPhoto(saved.capture());
        assertThat(saved.getAllValues()).extracting(ChatMessagePhoto::getSortSeq)
                .containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertThat(saved.getAllValues().get(0).getChatMsgSn()).isEqualTo(77L);
    }

    @Test
    void 사진이_없거나_열한_장이면_거부한다() {
        assertError(() -> service.send(OWNER, ROOM, photos(0)), ErrorCode.INVALID_INPUT_VALUE);
        assertError(() -> service.send(OWNER, ROOM, photos(11)), ErrorCode.INVALID_INPUT_VALUE);
        verify(fileStore, never()).store(any(MultipartFile.class), any(), any(), anyString());
        verify(chatMessageMapper, never()).insertMessage(any());
    }

    @Test
    void 주소_공유는_매칭_완료부터_수령_대기까지만_된다() {
        for (String allowed : List.of("PCAC0013", "PCAC0014", "PCAC0015")) {
            stubMatchStatus(allowed);
            assertThat(service.send(OWNER, ROOM, address("서곡공원", "정문 앞")).created()).isTrue();
        }
        for (String denied : List.of("PCAC0012", "PCAC0016", "PCAC0017", "PCAC0018")) {
            stubMatchStatus(denied);
            assertError(() -> service.send(OWNER, ROOM, address("서곡공원", null)),
                    ErrorCode.CHAT_ADDRESS_NOT_ALLOWED);
        }
    }

    @Test
    void 매칭이_없으면_주소를_공유할_수_없다() {
        assertError(() -> service.send(OWNER, ROOM, address("서곡공원", null)), ErrorCode.CHAT_ADDRESS_NOT_ALLOWED);
    }

    @Test
    void 주소는_장소와_상세_주소를_한_암호문으로_저장하고_응답에서_나눈다() {
        stubMatchStatus("PCAC0013");

        ChatMessageResponse res = service.send(OWNER, ROOM, address("서곡공원", "정문 앞"));

        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageMapper).insertMessage(saved.capture());
        assertThat(saved.getValue().getShrnAddrEnc()).isEqualTo("ENC:서곡공원\n정문 앞");
        assertThat(saved.getValue().getMsgCnEnc()).isNull();
        assertThat(res.shrnAddr()).isEqualTo("서곡공원");
        assertThat(res.dtlAddr()).isEqualTo("정문 앞");
    }

    @Test
    void 장소가_비어_있으면_주소_공유를_거부한다() {
        stubMatchStatus("PCAC0013");

        assertError(() -> service.send(OWNER, ROOM, address(" ", "정문")), ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    void 참여자가_아니거나_나간_사람은_보낼_수_없다() {
        assertError(() -> service.send(99L, ROOM, text("안녕", "m-1")), ErrorCode.CHAT_ROOM_ACCESS_DENIED);

        stubParticipant("CHTC0004");
        assertError(() -> service.send(OWNER, ROOM, text("안녕", "m-1")), ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        verify(chatMessageMapper, never()).insertMessage(any());
    }

    @Test
    void 없는_방은_404다() {
        assertError(() -> service.send(OWNER, 999L, text("안녕", "m-1")), ErrorCode.CHAT_ROOM_NOT_FOUND);
    }

    @Test
    void 시스템_메시지는_보낸_회원_없이_저장하고_실시간으로_보낸다() {
        ChatMessageSummary summary = service.createSystemMessage(ROOM, "매칭을 요청했어요.");

        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageMapper).insertMessage(saved.capture());
        assertThat(saved.getValue().getMbrSn()).isNull();
        assertThat(saved.getValue().getMsgTypeCd()).isEqualTo("CHTC0009");
        assertThat(saved.getValue().getIdmptDmndId()).startsWith("SYS-");
        assertThat(summary.getMsgCn()).isEqualTo("매칭을 요청했어요.");
        verify(chatRealtimePublisher).publishMessage(eq(ROOM), eq(List.of(OWNER, SITTER)), any());
    }

    @Test
    void 조회는_size_더하기_한_건으로_다음_페이지_여부를_정한다() {
        List<ChatMessageSummary> rows = new ArrayList<>();
        for (long sn = 40; sn >= 38; sn--) {
            ChatMessageSummary row = new ChatMessageSummary();
            row.setChatMsgSn(sn);
            row.setMbrSn(sn == 39 ? SITTER : OWNER);
            row.setMsgTypeCd("CHTC0005");
            row.setMsgCn("ENC:글" + sn);
            rows.add(row);
        }
        when(chatMessageMapper.selectList(ROOM, 41L, 3)).thenReturn(rows);
        ChatMessagePhoto photo = new ChatMessagePhoto();
        photo.setChatMsgSn(40L);
        photo.setAtchFileSn(600L);
        photo.setSortSeq(0);
        when(chatMessageMapper.selectPhotos(List.of(40L, 39L))).thenReturn(List.of(photo));

        ChatMessageSliceResponse res = service.search(OWNER, ROOM, 41L, 2);

        assertThat(res.hasNext()).isTrue();
        assertThat(res.content()).extracting(ChatMessageSummary::getChatMsgSn).containsExactly(40L, 39L);
        assertThat(res.content()).extracting(ChatMessageSummary::getMine).containsExactly(true, false);
        assertThat(res.content().get(0).getMsgCn()).isEqualTo("글40");
        assertThat(res.content().get(0).getPhotos()).hasSize(1);
        assertThat(res.content().get(1).getPhotos()).isEmpty();
    }

    @Test
    void 조회_크기는_오십을_넘기지_않고_참여자만_읽는다() {
        service.search(OWNER, ROOM, null, 500);
        verify(chatMessageMapper).selectList(ROOM, null, 51);

        assertError(() -> service.search(99L, ROOM, null, null), ErrorCode.CHAT_ROOM_ACCESS_DENIED);
    }
}
