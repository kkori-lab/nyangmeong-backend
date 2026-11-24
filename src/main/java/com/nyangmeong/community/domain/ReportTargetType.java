package com.nyangmeong.community.domain;

/**
 * [신고 대상 종류]
 * - 코드 없음 : DCLR.PST_SN·CMNT_SN·OTO_CHAT_ROOM_SN 중 값이 있는 쪽
 * - MEMBER : 채팅방의 상대 회원 (DCLR.TRGT_MBR_SN)
 */
public enum ReportTargetType {
    POST, COMMENT, MEMBER
}
