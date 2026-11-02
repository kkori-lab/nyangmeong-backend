package com.nyangmeong.member.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [관리자 정지 회원 목록 한 줄]
 * - SCR-ADM-012, 정지 일시·사유·처리 관리자는 최근 정지 이력 기준
 */
@Data
public class AdminSuspensionSummary {
    /** 회원일련번호 */
    private Long mbrSn;
    /** 회원닉네임 */
    private String mbrNcnm;
    /** 정지일시 */
    private LocalDateTime suspnDt;
    /** 정지종료일시 */
    private LocalDateTime suspnEndDt;
    /** 정지사유 */
    private String suspnRsn;
    /** 처리 관리자 닉네임 */
    private String prcrNcnm;
}
