package com.nyangmeong.global.log;

import java.time.LocalDate;

import lombok.Data;

/**
 * [관리자 작업 로그 검색 조건]
 * - 조건은 모두 선택
 */
@Data
public class AdminTaskLogSearchCond {
    /** 회원일련번호 */
    private Long mbrSn;
    /** 작업유형공통코드 */
    private String taskTypeCd;
    /** 대상테이블명 */
    private String trgtTblNm;
    /** 시작일자 */
    private LocalDate bgngYmd;
    /** 종료일자 */
    private LocalDate endYmd;
}
