package com.nyangmeong.admin.dto;

import java.time.LocalDate;

/**
 * [관리자 대시보드 일별 이용 통계]
 * - SCR-ADM-001 (FN-ADM-001-04) : 기간 안 날짜가 빠짐없이 나오고 없는 날은 0
 */
public record AdminDailyStatsSummary(
        /** 일자 */
        LocalDate ymd,
        /** 가입 수 */
        long joinCnt,
        /** 탈퇴 수 */
        long whdwlCnt,
        /** AI 대화(사용자 메시지) 수 */
        long aiChatCnt,
        /** OCR 요청 수 */
        long ocrRqstCnt) {
}
