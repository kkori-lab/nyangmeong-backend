package com.nyangmeong.community.service;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.community.domain.CareRecruitmentDetail;
import com.nyangmeong.community.domain.CareRecruitmentStatus;
import com.nyangmeong.community.domain.ChatMessageType;
import com.nyangmeong.community.domain.ChatParticipant;
import com.nyangmeong.community.domain.ChatParticipantStatus;
import com.nyangmeong.community.domain.ChatRoom;
import com.nyangmeong.community.domain.ChatRoomDetail;
import com.nyangmeong.community.domain.ChatRoomStatus;
import com.nyangmeong.community.domain.ChatStatusHistory;
import com.nyangmeong.community.domain.PostStatus;
import com.nyangmeong.community.dto.ChatRoomCreateRequest;
import com.nyangmeong.community.dto.ChatRoomReadRequest;
import com.nyangmeong.community.dto.ChatRoomResponse;
import com.nyangmeong.community.dto.ChatRoomSearchCond;
import com.nyangmeong.community.dto.ChatRoomSummary;
import com.nyangmeong.community.dto.ChatUnreadCountResponse;
import com.nyangmeong.community.mapper.CareRecruitmentMapper;
import com.nyangmeong.community.mapper.ChatRoomMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.global.security.crypto.FieldCryptoService;

import lombok.RequiredArgsConstructor;

/**
 * [펫돌봄 채팅방 서비스]
 * - SCR-CMU-004 채팅하기 : 게시글·보호자·지원자 조합에 방이 있으면 그 방, 없으면 새로 만든다
 * - 채팅 = 지원이라 별도 지원 절차가 없고, 지원자 수는 첫 메시지가 오간 방만 센다
 * - SCR-CMU-005 채팅 목록·읽음·나가기·사진 보기 : 읽음 위치는 CHAT_PRTPNT.LAST_PRSL_MSG_SEQ 에 두고 두 참여자 연결로 READ 전송
 * - 메시지는 ChatMessageService, 매칭은 CareMatchService
 */
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private static final String TAB_ALL = "ALL";
    private static final String PHOTO_PREVIEW = "사진을 보냈어요.";
    private static final String ADDRESS_PREVIEW = "주소를 공유했어요.";

    private final ChatRoomMapper chatRoomMapper;
    private final CareRecruitmentMapper careRecruitmentMapper;
    private final ChatRoomGuard chatRoomGuard;
    private final CareMatchService careMatchService;
    private final ChatRealtimePublisher chatRealtimePublisher;
    private final FieldCryptoService fieldCryptoService;
    private final FileStore fileStore;

    /**
     * 채팅방 조회 (참여자만)
     */
    @Transactional(readOnly = true)
    public ChatRoomResponse getRoom(Long mbrSn, Long otoChatRoomSn) {
        return ChatRoomResponse.of(chatRoomGuard.getRoom(mbrSn, otoChatRoomSn), mbrSn, false);
    }

    /**
     * 내 채팅 목록
     */
    @Transactional(readOnly = true)
    public List<ChatRoomSummary> search(Long mbrSn, ChatRoomSearchCond cond) {
        String tab = cond.getTab();
        if (tab != null && !TAB_ALL.equals(tab) && !ChatRoomSearchCond.TAB_MINE.equals(tab)
                && !ChatRoomSearchCond.TAB_APPLIED.equals(tab)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        List<ChatRoomSummary> rooms = chatRoomMapper.selectList(mbrSn, cond, ChatParticipantStatus.JOINED.getCode(),
                ChatRoomResponse.ROLE_GUARDIAN, ChatRoomResponse.ROLE_SITTER);
        for (ChatRoomSummary room : rooms) {
            room.setLastMsgCn(lastMessagePreview(room));
        }
        return rooms;
    }

    /**
     * 읽지 않은 메시지 총수
     */
    @Transactional(readOnly = true)
    public ChatUnreadCountResponse getUnreadCount(Long mbrSn) {
        return new ChatUnreadCountResponse(chatRoomMapper.countUnread(mbrSn, ChatParticipantStatus.JOINED.getCode()));
    }

    /**
     * 읽음 처리 (활성 대화창에 표시된 마지막 메시지까지)
     */
    @Transactional
    public void read(Long mbrSn, Long otoChatRoomSn, ChatRoomReadRequest req) {
        ChatRoomDetail room = chatRoomGuard.getRoom(mbrSn, otoChatRoomSn);
        if (chatRoomMapper.updateLastRead(otoChatRoomSn, mbrSn, req.lastPrslMsgSeq()) == 0) {
            return;
        }
        ChatParticipant participant = chatRoomMapper.selectParticipant(otoChatRoomSn, mbrSn);
        chatRealtimePublisher.publishRead(otoChatRoomSn, List.of(room.getOwnerMbrSn(), room.getCtptMbrSn()), mbrSn,
                participant.getLastPrslMsgSeq());
    }

    /**
     * 채팅방 나가기 (목록에서만 숨기고 기록은 유지, 진행 중 매칭은 취소)
     */
    @Transactional
    public void leave(Long mbrSn, Long otoChatRoomSn) {
        ChatRoomDetail room = chatRoomGuard.getRoom(mbrSn, otoChatRoomSn);
        ChatParticipant participant = chatRoomMapper.selectParticipant(otoChatRoomSn, mbrSn);
        if (participant == null || ChatParticipantStatus.LEFT.is(participant.getPtcpSttsCd())) {
            return;
        }
        careMatchService.cancelOnLeave(mbrSn, room);

        chatRoomMapper.updateParticipantStatus(otoChatRoomSn, mbrSn, ChatParticipantStatus.LEFT.getCode());
        insertParticipantHistory(participant.getChatPrtpntSn(), mbrSn, ChatParticipantStatus.JOINED,
                ChatParticipantStatus.LEFT);
    }

    /**
     * 채팅 사진 읽기 (방 참여자만)
     */
    @Transactional(readOnly = true)
    public StoredFile getImage(Long mbrSn, Long otoChatRoomSn, Long atchFileSn) {
        chatRoomGuard.getRoom(mbrSn, otoChatRoomSn);
        if (!chatRoomMapper.existsPhoto(otoChatRoomSn, atchFileSn)) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        return fileStore.load(atchFileSn);
    }

    /**
     * 채팅방 생성 또는 기존 방 반환
     */
    @Transactional
    public ChatRoomResponse create(Long mbrSn, ChatRoomCreateRequest req) {
        /** 기존 방은 글이 닫히거나 숨겨져도 계속 연다 */
        ChatRoom existing = chatRoomMapper.selectByPstSnAndCtptMbrSn(req.pstSn(), mbrSn);
        if (existing != null) {
            rejoin(existing.getOtoChatRoomSn(), mbrSn);
            return ChatRoomResponse.of(chatRoomMapper.selectDetail(existing.getOtoChatRoomSn()), mbrSn, false);
        }

        CareRecruitmentDetail recruitment = careRecruitmentMapper.selectDetailByPstSn(req.pstSn(),
                PostStatus.NORMAL.getCode());
        if (recruitment == null) {
            throw new CustomException(ErrorCode.CARE_RECRUITMENT_NOT_FOUND);
        }
        if (mbrSn.equals(recruitment.getMbrSn())) {
            throw new CustomException(ErrorCode.CHAT_ROOM_OWN_POST);
        }
        if (!CareRecruitmentStatus.RECRUITING.is(recruitment.getRcrtSttsCd())) {
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_RECRUITING);
        }

        ChatRoom room = new ChatRoom();
        room.setPstSn(req.pstSn());
        room.setCtptMbrSn(mbrSn);
        room.setChatRoomSttsCd(ChatRoomStatus.ACTIVE.getCode());
        try {
            chatRoomMapper.insertRoom(room);
        } catch (DuplicateKeyException e) {
            /** 같은 회원의 동시 요청은 먼저 만들어진 방을 돌려줌, 일반 조회는 스냅샷이라 방이 안 보임 */
            ChatRoom winner = chatRoomMapper.selectByPstSnAndCtptMbrSnForShare(req.pstSn(), mbrSn);
            if (winner == null) {
                throw new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND);
            }
            return ChatRoomResponse.of(chatRoomMapper.selectDetailForShare(winner.getOtoChatRoomSn()), mbrSn, false);
        }
        insertRoomHistory(room.getOtoChatRoomSn(), mbrSn);
        insertParticipant(room.getOtoChatRoomSn(), recruitment.getMbrSn());
        insertParticipant(room.getOtoChatRoomSn(), mbrSn);
        return ChatRoomResponse.of(chatRoomMapper.selectDetail(room.getOtoChatRoomSn()), mbrSn, true);
    }

    private void insertRoomHistory(Long otoChatRoomSn, Long mbrSn) {
        ChatStatusHistory history = new ChatStatusHistory();
        history.setRefSn(otoChatRoomSn);
        history.setMbrSn(mbrSn);
        history.setAftrSttsCd(ChatRoomStatus.ACTIVE.getCode());
        chatRoomMapper.insertRoomStatusHistory(history);
    }

    private void insertParticipant(Long otoChatRoomSn, Long mbrSn) {
        ChatParticipant participant = new ChatParticipant();
        participant.setOtoChatRoomSn(otoChatRoomSn);
        participant.setMbrSn(mbrSn);
        participant.setPtcpSttsCd(ChatParticipantStatus.JOINED.getCode());
        chatRoomMapper.insertParticipant(participant);

        ChatStatusHistory history = new ChatStatusHistory();
        history.setRefSn(participant.getChatPrtpntSn());
        history.setMbrSn(mbrSn);
        history.setAftrSttsCd(ChatParticipantStatus.JOINED.getCode());
        chatRoomMapper.insertParticipantStatusHistory(history);
    }

    private void insertParticipantHistory(Long chatPrtpntSn, Long mbrSn, ChatParticipantStatus from,
            ChatParticipantStatus to) {
        ChatStatusHistory history = new ChatStatusHistory();
        history.setRefSn(chatPrtpntSn);
        history.setMbrSn(mbrSn);
        history.setBfrSttsCd(from.getCode());
        history.setAftrSttsCd(to.getCode());
        chatRoomMapper.insertParticipantStatusHistory(history);
    }

    /** 같은 글·상대로 다시 대화하면 나갔던 사람을 목록에 되돌림 */
    private void rejoin(Long otoChatRoomSn, Long mbrSn) {
        ChatParticipant participant = chatRoomMapper.selectParticipant(otoChatRoomSn, mbrSn);
        if (participant == null || !ChatParticipantStatus.LEFT.is(participant.getPtcpSttsCd())) {
            return;
        }
        chatRoomMapper.updateParticipantStatus(otoChatRoomSn, mbrSn, ChatParticipantStatus.JOINED.getCode());
        insertParticipantHistory(participant.getChatPrtpntSn(), mbrSn, ChatParticipantStatus.LEFT,
                ChatParticipantStatus.JOINED);
    }

    private String lastMessagePreview(ChatRoomSummary room) {
        if (room.getLastMsgTypeCd() == null) {
            return null;
        }
        if (ChatMessageType.PHOTO.is(room.getLastMsgTypeCd())) {
            return PHOTO_PREVIEW;
        }
        if (ChatMessageType.ADDRESS.is(room.getLastMsgTypeCd())) {
            return ADDRESS_PREVIEW;
        }
        return room.getLastMsgCn() == null ? null : fieldCryptoService.decrypt(room.getLastMsgCn());
    }
}
