package com.nyangmeong.member.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

/**
 * [관리자 회원 목록 한 줄]
 * - SCR-ADM-010, 이메일 암호문은 응답에서 제외
 */
@Data
public class AdminMemberSummary {
    /** 회원일련번호 */
    private Long mbrSn;
    /** 회원닉네임 */
    private String mbrNcnm;
    /** 계정이메일암호문 */
    @JsonIgnore
    private String acntEmlEnc;
    /** 마스킹한 계정이메일 */
    private String maskedAcntEml;
    /** 가입경로공통코드 */
    private String joinPathCd;
    /** 회원상태공통코드 */
    private String mbrSttsCd;
    /** 회원권한공통코드 */
    private String mbrAuthrtCd;
    /** 등록일시 */
    private LocalDateTime regDt;
    /** 최종로그인일시 */
    private LocalDateTime lastLgnDt;
}
