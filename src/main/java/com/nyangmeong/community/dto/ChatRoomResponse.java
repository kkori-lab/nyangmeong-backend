package com.nyangmeong.community.dto;

import com.nyangmeong.community.domain.ChatRoomDetail;

/**
 * [채팅방 응답]
 * - ctptMbr* : 보는 사람 기준 상대 회원(보호자에게는 지원자, 지원자에게는 보호자)
 * - myRole : GUARDIAN(게시글 작성자) 또는 SITTER(지원자)
 * - created : 이번 요청으로 방을 만들었는지
 */
public record ChatRoomResponse(
        Long otoChatRoomSn,
        String chatRoomSttsCd,
        Long pstSn,
        String pstTtl,
        Long petPrtcRcrtSn,
        String rcrtSttsCd,
        Long ctptMbrSn,
        String ctptMbrNcnm,
        Long ctptProfileAtchFileSn,
        String myRole,
        Long petPrtcMtchSn,
        String mtchDlngSttsCd,
        boolean created) {

    public static final String ROLE_GUARDIAN = "GUARDIAN";
    public static final String ROLE_SITTER = "SITTER";

    public static ChatRoomResponse of(ChatRoomDetail room, Long mbrSn, boolean created) {
        boolean guardian = mbrSn.equals(room.getOwnerMbrSn());
        return new ChatRoomResponse(room.getOtoChatRoomSn(), room.getChatRoomSttsCd(), room.getPstSn(),
                room.getPstTtl(), room.getPetPrtcRcrtSn(), room.getRcrtSttsCd(),
                guardian ? room.getCtptMbrSn() : room.getOwnerMbrSn(),
                guardian ? room.getApplicantNcnm() : room.getOwnerNcnm(),
                guardian ? room.getApplicantAtchFileSn() : room.getOwnerAtchFileSn(),
                guardian ? ROLE_GUARDIAN : ROLE_SITTER, room.getPetPrtcMtchSn(), room.getMtchDlngSttsCd(), created);
    }
}
