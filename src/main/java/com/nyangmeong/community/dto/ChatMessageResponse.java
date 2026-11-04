package com.nyangmeong.community.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [채팅 메시지 전송 응답]
 * - created : 이번 요청으로 저장했는지(같은 idmptDmndId 재전송은 false, 200)
 */
public record ChatMessageResponse(
        Long chatMsgSn,
        Integer msgSeq,
        Long mbrSn,
        Boolean mine,
        String msgTypeCd,
        String msgCn,
        String shrnAddr,
        String dtlAddr,
        List<PostImageSummary> photos,
        LocalDateTime regDt,
        boolean created) {

    public static ChatMessageResponse of(ChatMessageSummary message, boolean created) {
        return new ChatMessageResponse(message.getChatMsgSn(), message.getMsgSeq(), message.getMbrSn(),
                message.getMine(), message.getMsgTypeCd(), message.getMsgCn(), message.getShrnAddr(),
                message.getDtlAddr(), message.getPhotos(), message.getRegDt(), created);
    }
}
