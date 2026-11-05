package com.nyangmeong.community.dto;

import java.util.List;

/**
 * [관리자 대시보드 게시글 현황]
 * - SCR-ADM-001 (FN-ADM-001-02) : 관리자 게시글 목록 기본 조건과 같게 정상·블라인드·삭제 글 전부
 */
public record AdminDashboardPostResponse(long totPstCnt, List<AdminRecentPostSummary> recentPsts) {
}
