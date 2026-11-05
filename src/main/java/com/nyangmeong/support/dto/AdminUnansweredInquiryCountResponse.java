package com.nyangmeong.support.dto;

/**
 * [관리자 대시보드 미답변 문의 수]
 * - SCR-ADM-001 (FN-ADM-001-03) : 삭제 문의 제외, 픽셀 캐릭터 유형은 전체 중 일부
 */
public record AdminUnansweredInquiryCountResponse(long unansInqryCnt, long pixelInqryCnt) {
}
