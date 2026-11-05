package com.nyangmeong.admin.dto;

import java.util.List;

import com.nyangmeong.community.dto.AdminDashboardPostResponse;
import com.nyangmeong.community.dto.AdminRecentPostSummary;
import com.nyangmeong.member.dto.AdminDashboardMemberSummary;
import com.nyangmeong.member.dto.AdminRecentMemberSummary;
import com.nyangmeong.support.dto.AdminUnansweredInquiryCountResponse;

/**
 * [관리자 대시보드 운영 현황·처리 대기]
 * - SCR-ADM-001 (FN-ADM-001-02·03) : 기간과 무관한 지금 값
 * - 처리 대기 건수 기준은 docs/api/admin.md
 */
public record AdminDashboardResponse(
        /** 전체 회원 수(정상 + 정지) */
        long totMbrCnt,
        /** 정상 회원 수 */
        long nrmlMbrCnt,
        /** 정지 회원 수 */
        long stopMbrCnt,
        /** 오늘 가입 수 */
        long todayJoinCnt,
        /** 최근 7일 가입 수 */
        long weekJoinCnt,
        /** 전체 게시글 수 */
        long totPstCnt,
        /** 최근 가입 회원 5명 */
        List<AdminRecentMemberSummary> recentMbrs,
        /** 최근 게시글 5건 */
        List<AdminRecentPostSummary> recentPsts,
        /** 접수 신고 수 */
        long dclrRcptCnt,
        /** 미답변 문의 수 */
        long unansInqryCnt,
        /** 미답변 문의 중 픽셀 캐릭터 유형 수 */
        long pixelInqryCnt,
        /** 캐릭터 생성 실패 수 */
        long charFailCnt,
        /** OCR 인식 실패 수 */
        long ocrFailCnt,
        /** 최근 24시간 알림 발송 실패 수 */
        long notiFailCnt,
        /** 마지막 실행이 실패한 배치 수 */
        long btchFailCnt) {

    /**
     * 도메인별 현황으로 응답 생성
     */
    public static AdminDashboardResponse of(AdminDashboardMemberSummary member, AdminDashboardPostResponse post,
            long dclrRcptCnt, AdminUnansweredInquiryCountResponse inquiry, long charFailCnt, long ocrFailCnt,
            long notiFailCnt, long btchFailCnt) {
        return new AdminDashboardResponse(member.getTotMbrCnt(), member.getNrmlMbrCnt(), member.getStopMbrCnt(),
                member.getTodayJoinCnt(), member.getWeekJoinCnt(), post.totPstCnt(),
                member.getRecentMbrs() == null ? List.of() : member.getRecentMbrs(), post.recentPsts(),
                dclrRcptCnt, inquiry.unansInqryCnt(), inquiry.pixelInqryCnt(), charFailCnt, ocrFailCnt,
                notiFailCnt, btchFailCnt);
    }
}
