package com.nyangmeong.community.domain;

/**
 * [채팅 메시지 유형 코드]
 * - CHTG03 (CHAT_MSG.MSG_TYPE_CD)
 */
public enum ChatMessageType {
    TEXT("CHTC0005"),
    PHOTO("CHTC0006"),
    ADDRESS("CHTC0007"),
    TRADE_CARD("CHTC0008"),
    SYSTEM("CHTC0009");

    private final String code;

    ChatMessageType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 회원이 직접 보낼 수 있는 유형 여부
     */
    public static boolean isSendable(String code) {
        return TEXT.is(code) || PHOTO.is(code) || ADDRESS.is(code);
    }
}
