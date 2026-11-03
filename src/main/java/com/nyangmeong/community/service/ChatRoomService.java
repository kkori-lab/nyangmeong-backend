package com.nyangmeong.community.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.community.domain.CareRecruitmentDetail;
import com.nyangmeong.community.domain.CareRecruitmentStatus;
import com.nyangmeong.community.domain.ChatParticipant;
import com.nyangmeong.community.domain.ChatParticipantStatus;
import com.nyangmeong.community.domain.ChatRoom;
import com.nyangmeong.community.domain.ChatRoomStatus;
import com.nyangmeong.community.domain.ChatStatusHistory;
import com.nyangmeong.community.domain.PostStatus;
import com.nyangmeong.community.dto.ChatRoomCreateRequest;
import com.nyangmeong.community.dto.ChatRoomResponse;
import com.nyangmeong.community.mapper.CareRecruitmentMapper;
import com.nyangmeong.community.mapper.ChatRoomMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * [펫돌봄 채팅방 서비스]
 * - SCR-CMU-004 채팅하기 : 게시글·보호자·지원자 조합에 방이 있으면 그 방, 없으면 새로 만든다
 * - 채팅 = 지원이라 별도 지원 절차가 없고, 지원자 수는 첫 메시지가 오간 방만 센다
 * - 메시지·채팅 목록·매칭은 SCR-CMU-005
 */
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomMapper chatRoomMapper;
    private final CareRecruitmentMapper careRecruitmentMapper;

    /**
     * 채팅방 생성 또는 기존 방 반환
     */
    @Transactional
    public ChatRoomResponse create(Long mbrSn, ChatRoomCreateRequest req) {
        /** 기존 방은 글이 닫히거나 숨겨져도 계속 연다 */
        ChatRoom existing = chatRoomMapper.selectByPstSnAndCtptMbrSn(req.pstSn(), mbrSn);
        if (existing != null) {
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
}
