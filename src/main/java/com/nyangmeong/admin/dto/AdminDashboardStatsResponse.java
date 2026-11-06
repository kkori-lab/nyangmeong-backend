package com.nyangmeong.admin.dto;

import java.util.List;

import com.nyangmeong.community.dto.AdminBoardPostCountSummary;
import com.nyangmeong.expense.dto.AdminExpenseCategoryCountSummary;
import com.nyangmeong.pet.dto.AdminPetDistributionSummary;
import com.nyangmeong.place.dto.AdminFavoritePlaceSummary;

/**
 * [관리자 대시보드 이용 통계]
 * - SCR-ADM-001 (FN-ADM-001-04) : 건수만 준다, 건강기록·AI 대화 내용은 없다 (결정 26)
 * - 종·품종 분포와 즐겨찾기 상위 장소는 기간 무관 지금 값
 */
public record AdminDashboardStatsResponse(
        /** 일별 가입·탈퇴·AI 대화·OCR 요청 수 */
        List<AdminDailyStatsSummary> daily,
        /** 게시판별 게시글 수 */
        List<AdminBoardPostCountSummary> pstCntByBbs,
        /** 가계부 카테고리별 지출 건수 */
        List<AdminExpenseCategoryCountSummary> expnsCntByCtgry,
        /** 반려동물 종·품종 분포 */
        List<AdminPetDistributionSummary> petDstrb,
        /** 즐겨찾기 상위 장소 */
        List<AdminFavoritePlaceSummary> fvrtPlcs) {
}
