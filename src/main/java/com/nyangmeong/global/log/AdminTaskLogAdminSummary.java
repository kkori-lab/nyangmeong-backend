package com.nyangmeong.global.log;

import lombok.Data;

/**
 * [관리자 필터 목록 응답]
 */
@Data
public class AdminTaskLogAdminSummary {
    /** 회원일련번호 */
    private Long mbrSn;
    /** 회원닉네임 */
    private String mbrNcnm;
}
