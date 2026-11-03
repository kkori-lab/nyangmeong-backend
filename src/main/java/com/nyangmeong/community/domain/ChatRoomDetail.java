package com.nyangmeong.community.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * [채팅방 상세 조회 결과]
 * - OTO_CHAT_ROOM 에 게시글·펫돌봄 모집·매칭·두 회원을 조인한 서비스 내부용, 응답으로 그대로 반환하지 않는다
 * - ctptMbrSn 은 지원자, 보호자는 게시글 작성자(ownerMbrSn)
 */
@Getter
@Setter
public class ChatRoomDetail extends ChatRoom {
    private String pstTtl;
    private Long ownerMbrSn;
    private String ownerNcnm;
    private Long ownerAtchFileSn;
    private String applicantNcnm;
    private Long applicantAtchFileSn;
    private Long petPrtcRcrtSn;
    private String rcrtSttsCd;
    private Long petPrtcMtchSn;
    private String mtchDlngSttsCd;
}
