package com.nyangmeong.member.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

/**
 * [관리자 탈퇴 회원 목록 한 줄]
 * - SCR-ADM-013, 이메일은 마스킹 값만 응답한다
 */
@Data
public class AdminWithdrawnMemberSummary {
    /** 회원일련번호 */
    private Long mbrSn;
    /** 회원닉네임 */
    private String mbrNcnm;
    /** 계정이메일암호문 */
    @JsonIgnore
    private String acntEmlEnc;
    /** 마스킹한 계정이메일 */
    private String maskedAcntEml;
    /** 탈퇴일시 */
    private LocalDateTime whdwlDt;
    /** 파기 예정일 */
    private LocalDate purgeYmd;
}
