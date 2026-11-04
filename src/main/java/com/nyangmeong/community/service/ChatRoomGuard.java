package com.nyangmeong.community.service;

import org.springframework.stereotype.Component;

import com.nyangmeong.community.domain.ChatParticipant;
import com.nyangmeong.community.domain.ChatParticipantStatus;
import com.nyangmeong.community.domain.ChatRoomDetail;
import com.nyangmeong.community.mapper.ChatRoomMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * [채팅방 접근 검사]
 * - 참여자(나간 사람 포함)만 방을 읽고, 쓰기·거래 진행은 참여 중인 사람만
 * - 호출하는 서비스의 트랜잭션 안에서 쓴다
 */
@Component
@RequiredArgsConstructor
public class ChatRoomGuard {

    private final ChatRoomMapper chatRoomMapper;

    /**
     * 참여자 확인 후 채팅방 상세
     */
    public ChatRoomDetail getRoom(Long mbrSn, Long otoChatRoomSn) {
        ChatRoomDetail room = chatRoomMapper.selectDetail(otoChatRoomSn);
        if (room == null) {
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }
        if (!mbrSn.equals(room.getOwnerMbrSn()) && !mbrSn.equals(room.getCtptMbrSn())) {
            throw new CustomException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
        return room;
    }

    /**
     * 참여 중인 회원만 통과
     */
    public void checkJoined(Long mbrSn, Long otoChatRoomSn) {
        ChatParticipant participant = chatRoomMapper.selectParticipant(otoChatRoomSn, mbrSn);
        if (participant == null || !ChatParticipantStatus.JOINED.is(participant.getPtcpSttsCd())) {
            throw new CustomException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }

    /**
     * 게시글 작성자(보호자) 여부
     */
    public boolean isGuardian(ChatRoomDetail room, Long mbrSn) {
        return mbrSn.equals(room.getOwnerMbrSn());
    }
}
