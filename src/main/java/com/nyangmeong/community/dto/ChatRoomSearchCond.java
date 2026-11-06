package com.nyangmeong.community.dto;

import lombok.Data;

/**
 * [채팅 목록 검색 조건]
 * - tab : ALL(기본) · MINE(내 글로 온 채팅) · APPLIED(내가 지원한 글의 채팅)
 * - pstSn : 돌봄 상세 [채팅 목록 보기]로 열 때 그 글의 채팅만
 */
@Data
public class ChatRoomSearchCond {

    public static final String TAB_MINE = "MINE";
    public static final String TAB_APPLIED = "APPLIED";

    private String tab;
    private boolean unreadOnly;
    private Long pstSn;
}
