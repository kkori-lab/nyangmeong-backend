package com.nyangmeong.community.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.community.domain.ChatParticipant;
import com.nyangmeong.community.domain.ChatRoom;
import com.nyangmeong.community.domain.ChatRoomDetail;
import com.nyangmeong.community.domain.ChatStatusHistory;

/**
 * [일대일 채팅방 Mapper]
 */
@Mapper
public interface ChatRoomMapper {

    /**
     * 게시글·지원자 조합의 채팅방 조회 (없으면 null)
     */
    ChatRoom selectByPstSnAndCtptMbrSn(@Param("pstSn") Long pstSn, @Param("ctptMbrSn") Long ctptMbrSn);

    /**
     * 게시글·지원자 조합의 채팅방 잠금 읽기 (중복 키 뒤 다시 조회용, 스냅샷이 아닌 최신 커밋 값)
     */
    ChatRoom selectByPstSnAndCtptMbrSnForShare(@Param("pstSn") Long pstSn, @Param("ctptMbrSn") Long ctptMbrSn);

    /**
     * 채팅방 상세 조회 (게시글·모집·매칭·두 회원 조인)
     */
    ChatRoomDetail selectDetail(Long otoChatRoomSn);

    /**
     * 채팅방 상세 잠금 읽기 (중복 키 뒤 다시 조회용)
     */
    ChatRoomDetail selectDetailForShare(Long otoChatRoomSn);

    /**
     * 채팅방 등록
     */
    int insertRoom(ChatRoom room);

    /**
     * 채팅방 상태 이력 등록
     */
    int insertRoomStatusHistory(ChatStatusHistory history);

    /**
     * 채팅 참여자 등록
     */
    int insertParticipant(ChatParticipant participant);

    /**
     * 채팅 참여자 상태 이력 등록
     */
    int insertParticipantStatusHistory(ChatStatusHistory history);
}
