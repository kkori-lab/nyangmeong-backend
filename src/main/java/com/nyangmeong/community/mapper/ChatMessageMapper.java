package com.nyangmeong.community.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.community.domain.ChatMessage;
import com.nyangmeong.community.domain.ChatMessagePhoto;
import com.nyangmeong.community.dto.ChatMessageSummary;

/**
 * [채팅 메시지 Mapper]
 */
@Mapper
public interface ChatMessageMapper {

    /**
     * 같은 멱등 요청 ID 의 메시지 잠금 읽기 (스냅샷이 아닌 최신 커밋 값)
     */
    ChatMessage selectByIdmptDmndIdForShare(String idmptDmndId);

    /**
     * 방의 다음 메시지 순서 (방 행 잠금을 잡은 뒤 호출, 최신 커밋 값)
     */
    int selectNextMsgSeqForUpdate(Long otoChatRoomSn);

    /**
     * 메시지 등록
     */
    int insertMessage(ChatMessage message);

    /**
     * 메시지 사진 등록
     */
    int insertPhoto(ChatMessagePhoto photo);

    /**
     * 메시지 조회 (최신순, 암호문 그대로)
     */
    List<ChatMessageSummary> selectList(@Param("otoChatRoomSn") Long otoChatRoomSn,
            @Param("beforeChatMsgSn") Long beforeChatMsgSn, @Param("limit") int limit);

    /**
     * 메시지들의 사진 조회
     */
    List<ChatMessagePhoto> selectPhotos(@Param("chatMsgSns") List<Long> chatMsgSns);
}
