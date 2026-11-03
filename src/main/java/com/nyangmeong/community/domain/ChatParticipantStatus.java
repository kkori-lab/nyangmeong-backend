package com.nyangmeong.community.domain;

/**
 * [채팅 참여 상태 코드]
 * - CHTG02 (CHAT_PRTPNT.PTCP_STTS_CD)
 */
public enum ChatParticipantStatus {
    JOINED("CHTC0003"),
    LEFT("CHTC0004");

    private final String code;

    ChatParticipantStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }
}
