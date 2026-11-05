package com.nyangmeong.member.dto;

import java.util.List;

import lombok.Data;

/**
 * [관리자 대시보드 회원 현황]
 * - SCR-ADM-001 (FN-ADM-001-02) : 일반 회원 권한만
 * - 건수는 MyBatis가 채우고 최근 가입 회원은 service가 붙임
 */
@Data
public class AdminDashboardMemberSummary {
    /** 전체 회원 수(정상 + 정지) */
    private long totMbrCnt;
    /** 정상 회원 수 */
    private long nrmlMbrCnt;
    /** 정지 회원 수 */
    private long stopMbrCnt;
    /** 오늘 가입 수 */
    private long todayJoinCnt;
    /** 최근 7일 가입 수 */
    private long weekJoinCnt;
    /** 최근 가입 회원 */
    private List<AdminRecentMemberSummary> recentMbrs;
}
