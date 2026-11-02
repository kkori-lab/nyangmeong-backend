package com.nyangmeong.community.dto;

import lombok.Data;

/**
 * [관리자 댓글 검색 조건]
 * - 조건은 모두 선택
 */
@Data
public class AdminCommentSearchCond {
    /** 게시글일련번호 */
    private Long pstSn;
    /** 작성자 회원일련번호 */
    private Long mbrSn;
    /** 검색어 : 작성자 닉네임 부분 일치 */
    private String q;
}
