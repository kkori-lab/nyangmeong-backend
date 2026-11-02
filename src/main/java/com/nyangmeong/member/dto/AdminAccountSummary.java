package com.nyangmeong.member.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

/**
 * [관리자 계정 목록 한 줄]
 * - SCR-ADM-113, 이메일은 마스킹 값만 응답한다
 */
@Data
public class AdminAccountSummary {
    /** 회원일련번호 */
    private Long mbrSn;
    /** 회원닉네임 */
    private String mbrNcnm;
    /** 계정이메일암호문 */
    @JsonIgnore
    private String acntEmlEnc;
    /** 마스킹한 계정이메일 */
    private String maskedAcntEml;
    /** 관리자 지정일시 */
    private LocalDateTime apntDt;
}
