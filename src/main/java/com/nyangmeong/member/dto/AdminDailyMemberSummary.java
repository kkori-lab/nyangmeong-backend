package com.nyangmeong.member.dto;

import java.time.LocalDate;

import lombok.Data;

/**
 * [관리자 대시보드 일별 가입·탈퇴 수]
 * - SCR-ADM-001 (FN-ADM-001-04) : 가입·탈퇴가 하나도 없는 날은 행이 없다
 */
@Data
public class AdminDailyMemberSummary {
    /** 일자 */
    private LocalDate ymd;
    /** 가입 수 */
    private long joinCnt;
    /** 탈퇴 수 */
    private long whdwlCnt;
}
