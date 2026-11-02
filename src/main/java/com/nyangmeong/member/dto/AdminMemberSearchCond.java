package com.nyangmeong.member.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * [관리자 회원 검색 조건]
 * - 조건은 모두 선택
 */
@Data
public class AdminMemberSearchCond {
    /** 검색어 : @가 있으면 이메일 완전 일치, 없으면 닉네임 부분 일치 */
    private String q;
    /** 회원상태공통코드 */
    private String status;
    /** 가입경로공통코드 */
    private String joinPath;
    /** 회원권한공통코드 */
    private String role;
    /** 가입 시작일자 */
    private LocalDate bgngYmd;
    /** 가입 종료일자 */
    private LocalDate endYmd;
}
