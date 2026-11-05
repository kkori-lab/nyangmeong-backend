package com.nyangmeong.pet.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

/**
 * [관리자 포인트 원장 검색 조건]
 * - SCR-ADM-072, 조건은 모두 선택. 기간은 양 끝 날짜 포함
 */
@Data
public class AdminPointSearchCond {
    /** 회원일련번호 */
    private Long mbrSn;
    /** 회원닉네임 (부분 일치) */
    private String mbrNcnm;
    /** 포인트거래유형공통코드 (MYPG03) */
    private String pntDlngTypeCd;
    /** 시작일자 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDt;
    /** 종료일자 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDt;
}
