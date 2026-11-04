package com.nyangmeong.community.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * [채팅 메시지 항목]
 * - 목록 조회와 실시간 전송에 같이 쓴다
 * - msgCn·shrnAddr 는 DB 에서 암호문으로 받아 서비스가 복호화하고, dtlAddr 는 주소 문자열에서 나눈다
 * - mine 은 받는 사람 기준이라 전송할 때마다 만든다
 */
@Data
public class ChatMessageSummary {
    private Long chatMsgSn;
    private Integer msgSeq;
    private Long mbrSn;
    private Boolean mine;
    private String msgTypeCd;
    private String msgCn;
    private String shrnAddr;
    private String dtlAddr;
    private List<PostImageSummary> photos = List.of();
    private LocalDateTime regDt;

    /**
     * 받는 사람 기준 복사본
     */
    public ChatMessageSummary forViewer(Long viewerMbrSn) {
        ChatMessageSummary copy = new ChatMessageSummary();
        copy.setChatMsgSn(chatMsgSn);
        copy.setMsgSeq(msgSeq);
        copy.setMbrSn(mbrSn);
        copy.setMine(mbrSn != null && mbrSn.equals(viewerMbrSn));
        copy.setMsgTypeCd(msgTypeCd);
        copy.setMsgCn(msgCn);
        copy.setShrnAddr(shrnAddr);
        copy.setDtlAddr(dtlAddr);
        copy.setPhotos(photos);
        copy.setRegDt(regDt);
        return copy;
    }
}
