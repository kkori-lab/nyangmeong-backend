package com.nyangmeong.ai.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * [관리자 대시보드 일별 AI 대화 수]
 * - SCR-ADM-001 (FN-ADM-001-04) : 사용자 메시지 수만 센다, 내용은 읽지 않는다 (결정 26)
 */
@Data
public class AdminDailyAiChatSummary {
    /** 일자 */
    private LocalDate ymd;
    /** 사용자 메시지 수 */
    private long cnt;
}
