package com.nyangmeong.member.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 대시보드 최근 가입 회원 한 줄]
 * - SCR-ADM-001 (FN-ADM-001-02)
 */
@Data
public class AdminRecentMemberSummary {
    /** 회원일련번호 */
    private Long mbrSn;
    /** 회원닉네임 */
    private String mbrNcnm;
    /** 등록일시 */
    private LocalDateTime regDt;
}
