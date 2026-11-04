package com.nyangmeong.community.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.community.domain.ChatParticipant;
import com.nyangmeong.community.domain.ChatRoom;
import com.nyangmeong.community.domain.ChatRoomDetail;
import com.nyangmeong.community.domain.ChatStatusHistory;
import com.nyangmeong.community.dto.ChatRoomSearchCond;
import com.nyangmeong.community.dto.ChatRoomSummary;

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

    /**
     * 채팅방 행 잠금 (메시지 순서·거래 상태 변경을 방 단위로 직렬화)
     */
    ChatRoom selectRoomForUpdate(Long otoChatRoomSn);

    /**
     * 채팅 참여자 조회 (참여하지 않으면 null)
     */
    ChatParticipant selectParticipant(@Param("otoChatRoomSn") Long otoChatRoomSn, @Param("mbrSn") Long mbrSn);

    /**
     * 채팅 참여 상태 변경
     */
    int updateParticipantStatus(@Param("otoChatRoomSn") Long otoChatRoomSn, @Param("mbrSn") Long mbrSn,
            @Param("ptcpSttsCd") String ptcpSttsCd);

    /**
     * 참여 중인 회원 번호 목록
     */
    List<Long> selectParticipantMbrSns(@Param("otoChatRoomSn") Long otoChatRoomSn,
            @Param("joinedCd") String joinedCd);

    /**
     * 같은 글의 다른 채팅방 번호 목록
     */
    List<Long> selectOtherRoomSns(@Param("pstSn") Long pstSn, @Param("exceptRoomSn") Long exceptRoomSn);

    /**
     * 내 채팅 목록
     */
    List<ChatRoomSummary> selectList(@Param("mbrSn") Long mbrSn, @Param("cond") ChatRoomSearchCond cond,
            @Param("joinedCd") String joinedCd, @Param("guardianRole") String guardianRole,
            @Param("sitterRole") String sitterRole);

    /**
     * 내 안 읽은 메시지 총수
     */
    int countUnread(@Param("mbrSn") Long mbrSn, @Param("joinedCd") String joinedCd);

    /**
     * 마지막으로 읽은 메시지 순서 갱신 (저장값보다 클 때만, 방의 마지막 순서를 넘지 않게)
     */
    int updateLastRead(@Param("otoChatRoomSn") Long otoChatRoomSn, @Param("mbrSn") Long mbrSn,
            @Param("lastPrslMsgSeq") Integer lastPrslMsgSeq);

    /**
     * 방의 메시지에 붙은 사진인지 여부
     */
    boolean existsPhoto(@Param("otoChatRoomSn") Long otoChatRoomSn, @Param("atchFileSn") Long atchFileSn);
}
