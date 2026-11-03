package com.nyangmeong.member.dto;

import com.nyangmeong.member.domain.SuspensionType;

import lombok.Data;

/**
 * [관리자 정지 회원 검색 조건]
 * - 조건은 모두 선택
 */
@Data
public class AdminSuspensionSearchCond {
    /** 닉네임 부분 일치 */
    private String q;
    /** 정지 종류 */
    private SuspensionType type;
}
