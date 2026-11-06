package com.nyangmeong.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import com.nyangmeong.community.domain.CareRecruitmentDetail;
import com.nyangmeong.community.domain.ChatParticipant;
import com.nyangmeong.community.domain.ChatRoom;
import com.nyangmeong.community.domain.ChatRoomDetail;
import com.nyangmeong.community.domain.ChatStatusHistory;
import com.nyangmeong.community.dto.ChatRoomCreateRequest;
import com.nyangmeong.community.dto.ChatRoomReadRequest;
import com.nyangmeong.community.dto.ChatRoomResponse;
import com.nyangmeong.community.dto.ChatRoomSearchCond;
import com.nyangmeong.community.dto.ChatRoomSummary;
import com.nyangmeong.community.mapper.CareRecruitmentMapper;
import com.nyangmeong.community.mapper.ChatRoomMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.security.crypto.FieldCryptoService;

/**
 * [검사 - 펫돌봄 채팅하기]
 */
class ChatRoomServiceTest {

    private static final Long OWNER = 1L;
    private static final Long SITTER = 2L;
    private static final Long PST = 7L;
    private static final Long ROOM = 11L;
    private static final String NORMAL = "CMUC0003";

    private final ChatRoomMapper chatRoomMapper = mock(ChatRoomMapper.class);
    private final CareRecruitmentMapper careRecruitmentMapper = mock(CareRecruitmentMapper.class);
    private final CareMatchService careMatchService = mock(CareMatchService.class);
    private final ChatRealtimePublisher chatRealtimePublisher = mock(ChatRealtimePublisher.class);
    private final FieldCryptoService fieldCryptoService = mock(FieldCryptoService.class);
    private final FileStore fileStore = mock(FileStore.class);
    private final ChatRoomService service = new ChatRoomService(chatRoomMapper, careRecruitmentMapper,
            new ChatRoomGuard(chatRoomMapper), careMatchService, chatRealtimePublisher, fieldCryptoService,
            fileStore);

    private CareRecruitmentDetail recruitment(String sttsCd) {
        CareRecruitmentDetail detail = new CareRecruitmentDetail();
        detail.setPetPrtcRcrtSn(5L);
        detail.setPstSn(PST);
        detail.setMbrSn(OWNER);
        detail.setRcrtSttsCd(sttsCd);
        return detail;
    }

    private ChatRoomDetail roomDetail() {
        ChatRoomDetail detail = new ChatRoomDetail();
        detail.setOtoChatRoomSn(ROOM);
        detail.setPstSn(PST);
        detail.setCtptMbrSn(SITTER);
        detail.setChatRoomSttsCd("CHTC0001");
        detail.setPstTtl("산책 도와주실 분");
        detail.setOwnerMbrSn(OWNER);
        detail.setOwnerNcnm("토리맘");
        detail.setOwnerAtchFileSn(25L);
        detail.setApplicantNcnm("산책왕");
        detail.setApplicantAtchFileSn(26L);
        detail.setPetPrtcRcrtSn(5L);
        detail.setRcrtSttsCd("PCAC0001");
        return detail;
    }

    private ChatRoom room() {
        ChatRoom room = new ChatRoom();
        room.setOtoChatRoomSn(ROOM);
        room.setPstSn(PST);
        room.setCtptMbrSn(SITTER);
        return room;
    }

    private ChatRoomCreateRequest req() {
        return new ChatRoomCreateRequest(PST);
    }

    private void assertError(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(CustomException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    /** 새 방 만들기에 필요한 준비 */
    private void stubNewRoom() {
        when(careRecruitmentMapper.selectDetailByPstSn(PST, NORMAL)).thenReturn(recruitment("PCAC0001"));
        doAnswer(inv -> {
            inv.<ChatRoom>getArgument(0).setOtoChatRoomSn(ROOM);
            return 1;
        }).when(chatRoomMapper).insertRoom(any(ChatRoom.class));
        doAnswer(inv -> {
            inv.<ChatParticipant>getArgument(0).setChatPrtpntSn(100L);
            return 1;
        }).when(chatRoomMapper).insertParticipant(any(ChatParticipant.class));
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(roomDetail());
    }

    @Test
    void 새_방은_보호자와_지원자를_참여시켜_만든다() {
        stubNewRoom();

        ChatRoomResponse res = service.create(SITTER, req());

        assertThat(res.created()).isTrue();
        assertThat(res.myRole()).isEqualTo("SITTER");
        assertThat(res.ctptMbrSn()).isEqualTo(OWNER);
        assertThat(res.ctptMbrNcnm()).isEqualTo("토리맘");
        assertThat(res.ctptProfileAtchFileSn()).isEqualTo(25L);
        assertThat(res.petPrtcMtchSn()).isNull();

        ArgumentCaptor<ChatRoom> inserted = ArgumentCaptor.forClass(ChatRoom.class);
        verify(chatRoomMapper).insertRoom(inserted.capture());
        assertThat(inserted.getValue().getCtptMbrSn()).isEqualTo(SITTER);
        assertThat(inserted.getValue().getChatRoomSttsCd()).isEqualTo("CHTC0001");
        ArgumentCaptor<ChatParticipant> participants = ArgumentCaptor.forClass(ChatParticipant.class);
        verify(chatRoomMapper, times(2)).insertParticipant(participants.capture());
        assertThat(participants.getAllValues()).extracting(ChatParticipant::getMbrSn).containsExactly(OWNER, SITTER);
        verify(chatRoomMapper).insertRoomStatusHistory(any());
        verify(chatRoomMapper, times(2)).insertParticipantStatusHistory(any());
    }

    @Test
    void 같은_회원이_다시_요청하면_같은_방을_돌려준다() {
        when(chatRoomMapper.selectByPstSnAndCtptMbrSn(PST, SITTER)).thenReturn(room());
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(roomDetail());

        ChatRoomResponse res = service.create(SITTER, req());

        assertThat(res.created()).isFalse();
        assertThat(res.otoChatRoomSn()).isEqualTo(ROOM);
        verify(chatRoomMapper, never()).insertRoom(any());
    }

    @Test
    void 글이_닫히거나_숨겨져도_기존_방은_계속_돌려준다() {
        when(chatRoomMapper.selectByPstSnAndCtptMbrSn(PST, SITTER)).thenReturn(room());
        ChatRoomDetail detail = roomDetail();
        detail.setRcrtSttsCd("PCAC0002");
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(detail);

        ChatRoomResponse res = service.create(SITTER, req());

        assertThat(res.rcrtSttsCd()).isEqualTo("PCAC0002");
        verify(careRecruitmentMapper, never()).selectDetailByPstSn(any(), any());
    }

    @Test
    void 보호자가_보면_상대는_지원자다() {
        when(chatRoomMapper.selectByPstSnAndCtptMbrSn(PST, OWNER)).thenReturn(room());
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(roomDetail());

        ChatRoomResponse res = service.create(OWNER, req());

        assertThat(res.myRole()).isEqualTo("GUARDIAN");
        assertThat(res.ctptMbrSn()).isEqualTo(SITTER);
        assertThat(res.ctptMbrNcnm()).isEqualTo("산책왕");
    }

    @Test
    void 내_글에는_채팅할_수_없다() {
        when(careRecruitmentMapper.selectDetailByPstSn(PST, NORMAL)).thenReturn(recruitment("PCAC0001"));

        assertError(() -> service.create(OWNER, req()), ErrorCode.CHAT_ROOM_OWN_POST);
        verify(chatRoomMapper, never()).insertRoom(any());
    }

    @Test
    void 펫돌봄_글이_아니거나_삭제_블라인드_글이면_새_방은_404() {
        assertError(() -> service.create(SITTER, req()), ErrorCode.CARE_RECRUITMENT_NOT_FOUND);
        verify(chatRoomMapper, never()).insertRoom(any());
    }

    @Test
    void 모집이_끝난_글에는_새_채팅을_시작할_수_없다() {
        when(careRecruitmentMapper.selectDetailByPstSn(PST, NORMAL)).thenReturn(recruitment("PCAC0002"));

        assertError(() -> service.create(SITTER, req()), ErrorCode.CHAT_ROOM_NOT_RECRUITING);
        verify(chatRoomMapper, never()).insertRoom(any());
    }

    @Test
    void 동시_요청으로_중복_키가_나면_먼저_만들어진_방을_돌려준다() {
        stubNewRoom();
        doThrow(new DuplicateKeyException("UK_OTO_CHAT_ROOM_1")).when(chatRoomMapper).insertRoom(any(ChatRoom.class));
        when(chatRoomMapper.selectByPstSnAndCtptMbrSnForShare(PST, SITTER)).thenReturn(room());
        when(chatRoomMapper.selectDetailForShare(ROOM)).thenReturn(roomDetail());

        ChatRoomResponse res = service.create(SITTER, req());

        assertThat(res.created()).isFalse();
        assertThat(res.otoChatRoomSn()).isEqualTo(ROOM);
        verify(chatRoomMapper, never()).insertParticipant(any());
        verify(chatRoomMapper, times(1)).selectByPstSnAndCtptMbrSn(PST, SITTER);
    }

    @Test
    void 중복_키_뒤에도_방을_못_찾으면_NPE_대신_404() {
        stubNewRoom();
        doThrow(new DuplicateKeyException("UK_OTO_CHAT_ROOM_1")).when(chatRoomMapper).insertRoom(any(ChatRoom.class));

        assertError(() -> service.create(SITTER, req()), ErrorCode.CHAT_ROOM_NOT_FOUND);
    }

    private ChatParticipant participant(String ptcpSttsCd, int lastPrslMsgSeq) {
        ChatParticipant participant = new ChatParticipant();
        participant.setChatPrtpntSn(100L);
        participant.setPtcpSttsCd(ptcpSttsCd);
        participant.setLastPrslMsgSeq(lastPrslMsgSeq);
        return participant;
    }

    private ChatRoomSummary summary(String lastMsgTypeCd, String lastMsgCn) {
        ChatRoomSummary summary = new ChatRoomSummary();
        summary.setOtoChatRoomSn(ROOM);
        summary.setLastMsgTypeCd(lastMsgTypeCd);
        summary.setLastMsgCn(lastMsgCn);
        return summary;
    }

    @Test
    void 참여자만_채팅방을_조회한다() {
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(roomDetail());

        assertThat(service.getRoom(OWNER, ROOM).myRole()).isEqualTo("GUARDIAN");
        assertThat(service.getRoom(SITTER, ROOM).myRole()).isEqualTo("SITTER");
        assertError(() -> service.getRoom(99L, ROOM), ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        assertError(() -> service.getRoom(OWNER, 999L), ErrorCode.CHAT_ROOM_NOT_FOUND);
    }

    @Test
    void 읽음_처리하면_저장된_위치를_두_참여자에게_알린다() {
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(roomDetail());
        when(chatRoomMapper.updateLastRead(ROOM, SITTER, 7)).thenReturn(1);
        when(chatRoomMapper.selectParticipant(ROOM, SITTER)).thenReturn(participant("CHTC0003", 7));

        service.read(SITTER, ROOM, new ChatRoomReadRequest(7));

        verify(chatRealtimePublisher).publishRead(ROOM, List.of(OWNER, SITTER), SITTER, 7);
    }

    @Test
    void 저장값보다_작은_읽음_위치는_무시하고_알리지_않는다() {
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(roomDetail());
        when(chatRoomMapper.updateLastRead(ROOM, SITTER, 2)).thenReturn(0);

        service.read(SITTER, ROOM, new ChatRoomReadRequest(2));

        verify(chatRealtimePublisher, never()).publishRead(any(), any(), any(), any());
    }

    @Test
    void 참여자가_아니면_읽음_처리할_수_없다() {
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(roomDetail());

        assertError(() -> service.read(99L, ROOM, new ChatRoomReadRequest(1)), ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        verify(chatRoomMapper, never()).updateLastRead(any(), any(), any());
    }

    @Test
    void 안_읽은_메시지_총수를_준다() {
        when(chatRoomMapper.countUnread(SITTER, "CHTC0003")).thenReturn(3);

        assertThat(service.getUnreadCount(SITTER).unreadCnt()).isEqualTo(3);
    }

    @Test
    void 목록은_마지막_메시지를_복호화하고_사진과_주소는_안내_문구로_바꾼다() {
        ChatRoomSearchCond cond = new ChatRoomSearchCond();
        when(chatRoomMapper.selectList(SITTER, cond, "CHTC0003", "GUARDIAN", "SITTER")).thenReturn(
                List.of(summary("CHTC0005", "ENC:안녕하세요"), summary("CHTC0006", null), summary("CHTC0007", null),
                        summary(null, null)));
        when(fieldCryptoService.decrypt("ENC:안녕하세요")).thenReturn("안녕하세요");

        List<ChatRoomSummary> rooms = service.search(SITTER, cond);

        assertThat(rooms).extracting(ChatRoomSummary::getLastMsgCn)
                .containsExactly("안녕하세요", "사진을 보냈어요.", "주소를 공유했어요.", null);
    }

    @Test
    void 목록_탭은_정해진_값만_받는다() {
        ChatRoomSearchCond cond = new ChatRoomSearchCond();
        cond.setTab("OTHER");

        assertError(() -> service.search(SITTER, cond), ErrorCode.INVALID_INPUT_VALUE);
        for (String tab : List.of("ALL", "MINE", "APPLIED")) {
            cond.setTab(tab);
            service.search(SITTER, cond);
        }
    }

    @Test
    void 나가면_진행_중인_매칭을_취소하고_참여_상태를_나감으로_바꾼다() {
        ChatRoomDetail detail = roomDetail();
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(detail);
        when(chatRoomMapper.selectParticipant(ROOM, SITTER)).thenReturn(participant("CHTC0003", 0));

        service.leave(SITTER, ROOM);

        verify(careMatchService).cancelOnLeave(SITTER, detail);
        verify(chatRoomMapper).updateParticipantStatus(ROOM, SITTER, "CHTC0004");
        ArgumentCaptor<ChatStatusHistory> history = ArgumentCaptor.forClass(ChatStatusHistory.class);
        verify(chatRoomMapper).insertParticipantStatusHistory(history.capture());
        assertThat(history.getValue().getBfrSttsCd()).isEqualTo("CHTC0003");
        assertThat(history.getValue().getAftrSttsCd()).isEqualTo("CHTC0004");
    }

    @Test
    void 이미_나간_방에서_다시_나가도_아무것도_바꾸지_않는다() {
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(roomDetail());
        when(chatRoomMapper.selectParticipant(ROOM, SITTER)).thenReturn(participant("CHTC0004", 0));

        service.leave(SITTER, ROOM);

        verify(careMatchService, never()).cancelOnLeave(any(), any());
        verify(chatRoomMapper, never()).updateParticipantStatus(any(), any(), any());
    }

    @Test
    void 나갔던_사람이_같은_글로_다시_채팅하면_목록에_되돌린다() {
        when(chatRoomMapper.selectByPstSnAndCtptMbrSn(PST, SITTER)).thenReturn(room());
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(roomDetail());
        when(chatRoomMapper.selectParticipant(ROOM, SITTER)).thenReturn(participant("CHTC0004", 0));

        ChatRoomResponse res = service.create(SITTER, req());

        assertThat(res.created()).isFalse();
        verify(chatRoomMapper).updateParticipantStatus(ROOM, SITTER, "CHTC0003");
    }

    @Test
    void 방에_붙지_않은_사진은_볼_수_없다() {
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(roomDetail());
        when(chatRoomMapper.existsPhoto(ROOM, 600L)).thenReturn(false);

        assertError(() -> service.getImage(SITTER, ROOM, 600L), ErrorCode.FILE_NOT_FOUND);
        assertError(() -> service.getImage(99L, ROOM, 600L), ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        verify(fileStore, never()).load(any());
    }

    @Test
    void 방에_붙은_사진은_참여자에게_준다() {
        when(chatRoomMapper.selectDetail(ROOM)).thenReturn(roomDetail());
        when(chatRoomMapper.existsPhoto(ROOM, 600L)).thenReturn(true);
        StoredFile file = new StoredFile(new byte[] { 1 }, "image/jpeg", "a.jpg");
        when(fileStore.load(600L)).thenReturn(file);

        assertThat(service.getImage(SITTER, ROOM, 600L)).isSameAs(file);
    }
}
