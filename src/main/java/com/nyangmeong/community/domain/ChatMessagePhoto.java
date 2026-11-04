package com.nyangmeong.community.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [채팅 메시지 사진 VO]
 * - CHAT_MSG_PHOTO 1:1
 */
@Data
public class ChatMessagePhoto {
    private Long chatMsgPhotoSn;
    private Long chatMsgSn;
    private Long atchFileSn;
    private Integer sortSeq;
    private LocalDateTime regDt;
}
