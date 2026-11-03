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

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import com.nyangmeong.community.domain.CareRecruitmentDetail;
import com.nyangmeong.community.domain.ChatParticipant;
import com.nyangmeong.community.domain.ChatRoom;
import com.nyangmeong.community.domain.ChatRoomDetail;
import com.nyangmeong.community.dto.ChatRoomCreateRequest;
import com.nyangmeong.community.dto.ChatRoomResponse;
import com.nyangmeong.community.mapper.CareRecruitmentMapper;
import com.nyangmeong.community.mapper.ChatRoomMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

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
    private final ChatRoomService service = new ChatRoomService(chatRoomMapper, careRecruitmentMapper);

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
}
