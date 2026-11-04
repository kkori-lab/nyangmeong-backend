package com.nyangmeong.community.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [채팅 목록 항목]
 * - ctptMbr* : 보는 사람 기준 상대 회원
 * - lastMsgCn : 서비스가 복호화하거나 사진·주소 안내 문구로 바꾼다
 */
@Data
public class ChatRoomSummary {
    private Long otoChatRoomSn;
    private String chatRoomSttsCd;
    private Long pstSn;
    private String pstTtl;
    private Long ctptMbrSn;
    private String ctptMbrNcnm;
    private Long ctptProfileAtchFileSn;
    private String myRole;
    private String mtchDlngSttsCd;
    private String lastMsgTypeCd;
    private String lastMsgCn;
    private LocalDateTime lastMsgDt;
    private Integer unreadCnt;
}
