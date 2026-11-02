package com.nyangmeong.member.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 회원 상세 약관 동의 한 줄]
 * - SCR-ADM-011, 약관 종류별 1행 : 동의 기록이 없으면 trmsVerId·agreDt 는 null
 */
@Data
public class AdminMemberTermsAgreeSummary {
    /** 약관고정식별키 */
    private String trmsFixIdntfKey;
    /** 약관제목 */
    private String trmsTtl;
    /** 필수동의여부 */
    private boolean esntlAgreYn;
    /** 동의한 약관버전 */
    private String trmsVerId;
    /** 동의여부 */
    private boolean agreYn;
    /** 동의일시 */
    private LocalDateTime agreDt;
}
