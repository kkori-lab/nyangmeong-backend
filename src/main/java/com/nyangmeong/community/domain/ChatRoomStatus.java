package com.nyangmeong.community.domain;

/**
 * [채팅방 상태 코드]
 * - CHTG01 (OTO_CHAT_ROOM.CHAT_ROOM_STTS_CD)
 */
public enum ChatRoomStatus {
    ACTIVE("CHTC0001"),
    READ_ONLY("CHTC0002");

    private final String code;

    ChatRoomStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
