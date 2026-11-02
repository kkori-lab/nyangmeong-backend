package com.nyangmeong.community.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * [관리자 게시글 검색 조건]
 * - 조건은 모두 선택
 */
@Data
public class AdminPostSearchCond {
    /** 검색어 : 제목·작성자 닉네임 부분 일치 */
    private String q;
    /** 게시판공통코드 */
    private String bbsCd;
    /** 게시글상태공통코드 */
    private String status;
    /** 작성 시작일자 */
    private LocalDate bgngYmd;
    /** 작성 종료일자 */
    private LocalDate endYmd;
}
